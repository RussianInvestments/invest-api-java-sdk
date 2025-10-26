package ru.ttech.piapi.core.impl.marketdata;

import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.Getter;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.SubscriptionAction;
import ru.tinkoff.piapi.contract.v1.TradeSourceType;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
import ru.ttech.piapi.core.impl.marketdata.subscription.CandleSubscriptionSpec;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;
import ru.ttech.piapi.core.impl.marketdata.subscription.MarketDataSubscriptionResult;
import ru.ttech.piapi.core.impl.marketdata.subscription.RequestAction;
import ru.ttech.piapi.core.impl.marketdata.subscription.SubscriptionStatus;
import ru.ttech.piapi.core.impl.marketdata.util.MarketDataRequestBuilder;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.LastPriceWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.OrderBookWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.TradeWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.TradingStatusWrapper;
import ru.ttech.piapi.core.impl.wrapper.ResponseWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Менеджер стримов рыночных данных
 */
public class MarketDataStreamManager {

  @Getter
  protected final StreamServiceStubFactory streamFactory;
  protected final ConnectorConfiguration configuration;
  protected final ScheduledExecutorService scheduledExecutorService;
  protected final ExecutorService executorService;
  protected final MarketDataStreamContext context;
  protected final List<MarketDataStreamWrapper> streamWrappers = Collections.synchronizedList(new ArrayList<>());
  protected final AtomicReference<CompletableFuture<MarketDataSubscriptionResult>> lastTask = new AtomicReference<>();

  public MarketDataStreamManager(
    StreamServiceStubFactory streamFactory,
    ExecutorService executorService,
    ScheduledExecutorService scheduledExecutorService
  ) {
    this.streamFactory = streamFactory;
    this.configuration = streamFactory.getServiceStubFactory().getConfiguration();
    this.context = new MarketDataStreamContext();
    this.executorService = executorService;
    this.scheduledExecutorService = scheduledExecutorService;
    this.lastTask.set(CompletableFuture.completedFuture(null));
  }

  /**
   * Метод для запуска менеджера стримов
   */
  public void start() {
    executorService.submit(() -> startListenersProcessing(context.getCandleQueue(), context.getOnCandleListeners()));
    executorService.submit(() -> startListenersProcessing(context.getLastPriceQueue(), context.getOnLastPriceListeners()));
    executorService.submit(() -> startListenersProcessing(context.getTradesQueue(), context.getOnTradeListeners()));
    executorService.submit(() -> startListenersProcessing(context.getOrderBooksQueue(), context.getOnOrderBookListeners()));
    executorService.submit(() -> startListenersProcessing(context.getTradingStatusesQueue(), context.getOnTradingStatusListeners()));
  }

  /**
   * Метод для подписки на свечи по списку инструментов
   * <p>Пример:
   * <pre>{@code
   * marketDataStreamManager.subscribeCandles(
   *       availableInstruments,
   *       new CandleSubscriptionSpec(),
   *       candle -> logger.info("New candle for instrument: {}", candle.getInstrumentUid())
   *     );
   * }</pre>
   *
   * @param instruments       список инструментов {@link Instrument}
   * @param subscriptionSpecs свойства подписки {@link CandleSubscriptionSpec}
   * @param onCandleListener  листенер свечей
   */
  public void subscribeCandles(
    Set<Instrument> instruments,
    CandleSubscriptionSpec subscriptionSpecs,
    OnNextListener<CandleWrapper> onCandleListener
  ) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = instrumentsSublist ->
      MarketDataRequestBuilder.buildCandlesRequest(
        instrumentsSublist,
        subscriptionSpecs.getCandleSource(),
        subscriptionSpecs.isWaitingClose(),
        SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE
      );
    var filteredInstruments = instruments.stream()
      .filter(instrument -> !isSubscribedCandles(instrument))
      .collect(Collectors.toUnmodifiableSet());
    subscribe(MarketDataResponseType.CANDLE, filteredInstruments, requestBuilder)
      .whenComplete((subscriptionResult, __) -> Optional.ofNullable(subscriptionResult)
        .ifPresent(result -> addWrapperListener(context.getOnCandleListeners(), onCandleListener))
      );
  }

  /**
   * Метод для отписки от свечей по списку инструментов
   *
   * @param instruments       список инструментов {@link Instrument}
   * @param subscriptionSpecs свойства подписки {@link CandleSubscriptionSpec}
   */
  public void unsubscribeCandles(Set<Instrument> instruments, CandleSubscriptionSpec subscriptionSpecs) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = instrumentsSublist ->
      MarketDataRequestBuilder.buildCandlesRequest(
        instrumentsSublist,
        subscriptionSpecs.getCandleSource(),
        subscriptionSpecs.isWaitingClose(),
        SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE
      );
    unsubscribe(MarketDataResponseType.CANDLE, instruments, requestBuilder);
  }


  /**
   * Метод для подписки на последние цены по списку инструментов
   * <p>Пример:
   * <pre>{@code
   * marketDataStreamManager.subscribeLastPrices(
   *       availableInstruments,
   *       lastPrice -> logger.info("New last price incoming for instrument: {}", lastPrice.getInstrumentUid())
   *     );
   * }</pre>
   *
   * @param instruments         список инструментов {@link Instrument}
   * @param onLastPriceListener листенер последних цен
   */
  public void subscribeLastPrices(
    Set<Instrument> instruments,
    OnNextListener<LastPriceWrapper> onLastPriceListener
  ) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = sublist ->
      MarketDataRequestBuilder.buildLastPricesRequest(sublist, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE);
    var filteredInstruments = instruments.stream()
      .filter(instrument -> !isSubscribedLastPrice(instrument))
      .collect(Collectors.toUnmodifiableSet());
    subscribe(MarketDataResponseType.LAST_PRICE, filteredInstruments, requestBuilder)
      .whenComplete((subscriptionResult, __) -> Optional.ofNullable(subscriptionResult)
        .ifPresent(result -> addWrapperListener(context.getOnLastPriceListeners(), onLastPriceListener))
      );
  }

  /**
   * Метод для отписки от последних цен по списку инструментов
   *
   * @param instruments список инструментов {@link Instrument}
   */
  public void unsubscribeLastPrices(Set<Instrument> instruments) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = sublist ->
      MarketDataRequestBuilder.buildLastPricesRequest(sublist, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE);
    unsubscribe(MarketDataResponseType.LAST_PRICE, instruments, requestBuilder);
  }

  /**
   * Метод для подписки на сделки по списку инструментов
   * <p>Пример:
   * <pre>{@code
   *     marketDataStreamManager.subscribeTrades(
   *       availableInstruments,
   *       TradeSourceType.TRADE_SOURCE_ALL,
   *       trade -> logger.info("New trade incoming for instrument: {}", trade.getInstrumentUid())
   *     );
   * }</pre>
   *
   * @param instruments     список инструментов {@link Instrument}
   * @param tradeSourceType тип источника сделок
   * @param onTradeListener листенер сделок
   */
  public void subscribeTrades(
    Set<Instrument> instruments,
    TradeSourceType tradeSourceType,
    OnNextListener<TradeWrapper> onTradeListener
  ) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = instrumentsSublist ->
      MarketDataRequestBuilder.buildTradesRequest(instrumentsSublist, tradeSourceType, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE);
    var filteredInstruments = instruments.stream()
      .filter(instrument -> !isSubscribedTrades(instrument))
      .collect(Collectors.toUnmodifiableSet());
    subscribe(MarketDataResponseType.TRADE, filteredInstruments, requestBuilder)
      .whenComplete((subscriptionResult, __) -> Optional.ofNullable(subscriptionResult)
        .ifPresent(result -> addWrapperListener(context.getOnTradeListeners(), onTradeListener))
      );
  }

  /**
   * Метод для отписки от сделок по списку инструментов
   *
   * @param instruments     список инструментов {@link Instrument}
   * @param tradeSourceType тип источника сделок
   */
  public void unsubscribeTrades(Set<Instrument> instruments, TradeSourceType tradeSourceType) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = instrumentsSublist ->
      MarketDataRequestBuilder.buildTradesRequest(instrumentsSublist, tradeSourceType, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE);
    unsubscribe(MarketDataResponseType.TRADE, instruments, requestBuilder);
  }

  /**
   * Метод для подписки на торговые стаканы по списку инструментов
   * <p>Пример:
   * <pre>{@code
   *     marketDataStreamManager.subscribeOrderBooks(
   *       availableInstruments,
   *       orderBook -> logger.info("New order book incoming for instrument: {}", orderBook.getInstrumentUid())
   *     );
   * }</pre>
   *
   * @param instruments         список инструментов {@link Instrument}
   * @param onOrderBookListener листенер торговых стаканов
   */
  public void subscribeOrderBooks(
    Set<Instrument> instruments,
    OnNextListener<OrderBookWrapper> onOrderBookListener
  ) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = sublist ->
      MarketDataRequestBuilder.buildOrderBooksRequest(sublist, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE);
    var filteredInstruments = instruments.stream()
      .filter(instrument -> !isSubscribedOrderBook(instrument))
      .collect(Collectors.toUnmodifiableSet());
    subscribe(MarketDataResponseType.ORDER_BOOK, filteredInstruments, requestBuilder)
      .whenComplete((subscriptionResult, __) -> Optional.ofNullable(subscriptionResult)
        .ifPresent(result -> addWrapperListener(context.getOnOrderBookListeners(), onOrderBookListener))
      );
  }

  /**
   * Метод для отписки от торговых стаканов по списку инструментов
   *
   * @param instruments список инструментов {@link Instrument}
   */
  public void unsubscribeOrderBooks(Set<Instrument> instruments) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = sublist ->
      MarketDataRequestBuilder.buildOrderBooksRequest(sublist, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE);
    subscribe(MarketDataResponseType.ORDER_BOOK, instruments, requestBuilder);
  }

  /**
   * Метод для подписки на статусы торгов по списку инструментов
   * <p>Пример:
   * <pre>{@code
   *     marketDataStreamManager.subscribeTradingStatuses(
   *       availableInstruments,
   *       tradingStatus -> logger.info("New trading status incoming for instrument: {}", tradingStatus.getInstrumentUid())
   *     );
   * }</pre>
   *
   * @param instruments             список инструментов {@link Instrument}
   * @param onTradingStatusListener листенер статусов торгов
   */
  public void subscribeTradingStatuses(
    Set<Instrument> instruments,
    OnNextListener<TradingStatusWrapper> onTradingStatusListener
  ) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = sublist ->
      MarketDataRequestBuilder.buildTradingStatusesRequest(sublist, SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE);
    var filteredInstruments = instruments.stream()
      .filter(instrument -> !isSubscribedTradingStatuses(instrument))
      .collect(Collectors.toUnmodifiableSet());
    subscribe(MarketDataResponseType.TRADING_STATUS, filteredInstruments, requestBuilder)
      .whenComplete((subscriptionResult, __) -> Optional.ofNullable(subscriptionResult)
        .ifPresent(result -> addWrapperListener(context.getOnTradingStatusListeners(), onTradingStatusListener))
      );
  }

  /**
   * Метод для отписки от статусов торгов по списку инструментов
   *
   * @param instruments список инструментов {@link Instrument}
   */
  public void unsubscribeTradingStatuses(Set<Instrument> instruments) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = sublist ->
      MarketDataRequestBuilder.buildTradingStatusesRequest(sublist, SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE);
    subscribe(MarketDataResponseType.TRADING_STATUS, instruments, requestBuilder);
  }

  protected <T extends ResponseWrapper<?>> void addWrapperListener(
    List<OnNextListener<T>> listeners,
    OnNextListener<T> listener
  ) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  /**
   * Метод для проверки наличия подписки на свечи по инструменту
   *
   * @param instrument Инструмент
   * @return true если активная подписка есть, false если нет
   */
  public boolean isSubscribedCandles(Instrument instrument) {
    return checkInstrumentSubscription(MarketDataResponseType.CANDLE, instrument);
  }

  /**
   * Метод для проверки наличия подписки на последнюю цену по инструменту
   *
   * @param instrument Инструмент
   * @return true если активная подписка есть, false если нет
   */
  public boolean isSubscribedLastPrice(Instrument instrument) {
    return checkInstrumentSubscription(MarketDataResponseType.LAST_PRICE, instrument);
  }

  /**
   * Метод для проверки наличия подписки на сделки по инструменту
   *
   * @param instrument Инструмент
   * @return true если активная подписка есть, false если нет
   */
  public boolean isSubscribedTrades(Instrument instrument) {
    return checkInstrumentSubscription(MarketDataResponseType.TRADE, instrument);
  }

  /**
   * Метод для проверки наличия подписки на торговый стакан по инструменту
   *
   * @param instrument Инструмент
   * @return true если активная подписка есть, false если нет
   */
  public boolean isSubscribedOrderBook(Instrument instrument) {
    return checkInstrumentSubscription(MarketDataResponseType.ORDER_BOOK, instrument);
  }

  /**
   * Метод для проверки наличия подписки на статус торгов по инструменту
   *
   * @param instrument Инструмент
   * @return true если активная подписка есть, false если нет
   */
  public boolean isSubscribedTradingStatuses(Instrument instrument) {
    return checkInstrumentSubscription(MarketDataResponseType.TRADING_STATUS, instrument);
  }

  /**
   * Метод для завершения работы менеджера
   */
  public void shutdown() {
    streamWrappers.forEach(MarketDataStreamWrapper::disconnect);
  }

  protected boolean checkInstrumentSubscription(
    MarketDataResponseType responseType,
    Instrument instrument
  ) {
    return streamWrappers.stream()
      .anyMatch(wrapper -> wrapper.isSubscribed(responseType, instrument));
  }

  protected CompletableFuture<MarketDataSubscriptionResult> subscribe(
    MarketDataResponseType responseType,
    Set<Instrument> instruments,
    Function<List<Instrument>, MarketDataRequest> requestBuilder
  ) {
    if (instruments.isEmpty()) {
      return CompletableFuture.failedFuture(new IllegalArgumentException("Instruments list is empty"));
    }
    var supplier = Lazy.of(() -> CompletableFuture.supplyAsync(() -> {
      var instrumentsList = new ArrayList<>(instruments);
      var subscriptionResults = new HashMap<Instrument, SubscriptionStatus>();
      int i = 0;
      while (i < instruments.size()) {
        var streamWrapper = getAvailableStreamWrapper();
        int endIndex = Math.min(
          instruments.size(),
          i + configuration.getMaxMarketDataSubscriptionsCount() - streamWrapper.getSubscriptionsCount()
        );
        var sublist = instrumentsList.subList(i, endIndex);
        i = endIndex;
        var subscriptionResult = streamWrapper.newCall(requestBuilder.apply(sublist)).join();
        subscriptionResults.putAll(subscriptionResult.getSubscriptionStatusMap());
      }
      return new MarketDataSubscriptionResult(RequestAction.SUBSCRIBE, responseType, subscriptionResults);
    }));
    return lastTask.updateAndGet(previousTask -> previousTask.thenCompose(previousResult -> supplier.get()));
  }

  public void unsubscribe(
    MarketDataResponseType responseType,
    Set<Instrument> instruments,
    Function<List<Instrument>, MarketDataRequest> requestBuilder
  ) {
    var supplier = Lazy.of(() -> CompletableFuture.supplyAsync(() -> {
      var wrappersSubscriptions = linkSubscriptionsToWrappers(responseType, instruments);
      var subscriptionResults = new HashMap<Instrument, SubscriptionStatus>();
      wrappersSubscriptions.forEach((wrapper, sublist) -> {
        var subscriptionResult = wrapper.newCall(requestBuilder.apply(sublist)).join();
        subscriptionResults.putAll(subscriptionResult.getSubscriptionStatusMap());
      });
      return new MarketDataSubscriptionResult(RequestAction.UNSUBSCRIBE, responseType, subscriptionResults);
    }));
    lastTask.updateAndGet(previousTask -> previousTask.thenCompose(previousResult -> supplier.get()));
  }

  protected Map<MarketDataStreamWrapper, List<Instrument>> linkSubscriptionsToWrappers(
    MarketDataResponseType responseType,
    Set<Instrument> instruments
  ) {
    return streamWrappers.stream()
      .flatMap(wrapper -> instruments.stream()
        .filter(instrument -> wrapper.isSubscribed(responseType, instrument))
        .map(instrument -> Tuple.of(wrapper, instrument))
      )
      .collect(Collectors.groupingBy(Tuple2::_1, Collectors.mapping(Tuple2::_2, Collectors.toList())));
  }

  protected MarketDataStreamWrapper getAvailableStreamWrapper() {
    return streamWrappers.stream()
      .filter(wrapper -> wrapper.getSubscriptionsCount() < configuration.getMaxMarketDataSubscriptionsCount())
      .findAny()
      .orElseGet(() -> {
        if (streamWrappers.size() >= configuration.getMaxMarketDataStreamsCount()) {
          throw new IllegalStateException("No available stream wrappers");
        }
        var newWrapper = createStreamWrapper();
        streamWrappers.add(newWrapper);
        return newWrapper;
      });
  }

  protected MarketDataStreamWrapper createStreamWrapper() {
    var configuration = MarketDataStreamWrapperConfiguration.builder(scheduledExecutorService)
      .addOnCandleListener(context.getGlobalOnCandleListener())
      .addOnLastPriceListener(context.getGlobalOnLastPriceListener())
      .addOnOrderBookListener(context.getGlobalOnOrderBookListener())
      .addOnTradeListener(context.getGlobalOnTradeListener())
      .addOnTradingStatusListener(context.getGlobalOnTradingStatusesListener())
      .build();
    return new MarketDataStreamWrapper(streamFactory, configuration);
  }

  protected <T extends ResponseWrapper<?>> void startListenersProcessing(
    BlockingQueue<T> queue,
    List<OnNextListener<T>> listeners
  ) {
    while (true) {
      try {
        var wrapper = queue.take();
        listeners.forEach(listener -> listener.onNext(wrapper));
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
