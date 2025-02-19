package ru.ttech.piapi.core.impl.marketdata;

import io.vavr.Lazy;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.TradeSourceType;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
import ru.ttech.piapi.core.impl.marketdata.subscription.CandleSubscriptionSpec;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;
import ru.ttech.piapi.core.impl.marketdata.subscription.SubscriptionResult;
import ru.ttech.piapi.core.impl.marketdata.subscription.SubscriptionStatus;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Менеджер стримов рыночных данных
 */
public class MarketDataStreamManager {

  private final StreamServiceStubFactory streamFactory;
  private final ConnectorConfiguration configuration;
  private final ExecutorService executor;
  private final MarketDataStreamContext context;
  private final List<MarketDataStreamWrapper> streamWrappers = Collections.synchronizedList(new ArrayList<>());
  private final AtomicReference<CompletableFuture<SubscriptionResult>> lastTask = new AtomicReference<>();

  public MarketDataStreamManager(StreamServiceStubFactory streamFactory, ExecutorService executorService) {
    this.streamFactory = streamFactory;
    this.configuration = streamFactory.getServiceStubFactory().getConfiguration();
    this.context = new MarketDataStreamContext();
    this.executor = executorService;
    this.lastTask.set(CompletableFuture.completedFuture(null));
  }

  /**
   * Метод для запуска менеджера стримов
   */
  public void start() {
    executor.submit(() -> startListenersProcessing(context.getCandleQueue(), context.getOnCandleListeners()));
    executor.submit(() -> startListenersProcessing(context.getLastPriceQueue(), context.getOnLastPriceListeners()));
    executor.submit(() -> startListenersProcessing(context.getTradesQueue(), context.getOnTradeListeners()));
    executor.submit(() -> startListenersProcessing(context.getOrderBooksQueue(), context.getOnOrderBookListeners()));
    executor.submit(() -> startListenersProcessing(context.getTradingStatusesQueue(), context.getOnTradingStatusListeners()));
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
        subscriptionSpecs.isWaitingClose()
      );
    var filteredInstruments = instruments.stream()
      .filter(instrument -> !context.getCandlesSubscriptionsMap().containsKey(instrument))
      .collect(Collectors.toList());
    subscribe(MarketDataResponseType.CANDLE, filteredInstruments, requestBuilder)
      .whenComplete((subscriptionResult, __) -> Optional.ofNullable(subscriptionResult).ifPresent(result -> {
        context.getCandlesSubscriptionsMap().putAll(result.getSubscriptionStatusMap());
        context.getOnCandleListeners().add(onCandleListener);
      }));
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
    Function<List<Instrument>, MarketDataRequest> requestBuilder = MarketDataRequestBuilder::buildLastPricesRequest;
    var filteredInstruments = instruments.stream()
      .filter(instrument -> !context.getLastPricesSubscriptionsMap().containsKey(instrument))
      .collect(Collectors.toList());
    subscribe(MarketDataResponseType.LAST_PRICE, filteredInstruments, requestBuilder)
      .whenComplete((subscriptionResult, __) -> Optional.ofNullable(subscriptionResult).ifPresent(result -> {
        context.getLastPricesSubscriptionsMap().putAll(result.getSubscriptionStatusMap());
        context.getOnLastPriceListeners().add(onLastPriceListener);
      }));
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
      MarketDataRequestBuilder.buildTradesRequest(instrumentsSublist, tradeSourceType);
    var filteredInstruments = instruments.stream()
      .filter(instrument -> !context.getTradesSubscriptionsMap().containsKey(instrument))
      .collect(Collectors.toList());
    subscribe(MarketDataResponseType.TRADE, filteredInstruments, requestBuilder)
      .whenComplete((subscriptionResult, __) -> Optional.ofNullable(subscriptionResult).ifPresent(result -> {
        context.getTradesSubscriptionsMap().putAll(result.getSubscriptionStatusMap());
        context.getOnTradeListeners().add(onTradeListener);
      }));
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
    Function<List<Instrument>, MarketDataRequest> requestBuilder = MarketDataRequestBuilder::buildOrderBooksRequest;
    var filteredInstruments = instruments.stream()
      .filter(instrument -> !context.getOrderBooksSubscriptionsMap().containsKey(instrument))
      .collect(Collectors.toList());
    subscribe(MarketDataResponseType.ORDER_BOOK, filteredInstruments, requestBuilder)
      .whenComplete((subscriptionResult, __) -> Optional.ofNullable(subscriptionResult).ifPresent(result -> {
        context.getOrderBooksSubscriptionsMap().putAll(result.getSubscriptionStatusMap());
        context.getOnOrderBookListeners().add(onOrderBookListener);
      }));
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
    List<Instrument> instruments,
    OnNextListener<TradingStatusWrapper> onTradingStatusListener
  ) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = MarketDataRequestBuilder::buildTradingStatusesRequest;
    var filteredInstruments = instruments.stream()
      .filter(instrument -> !context.getTradingStatusesSubscriptionsMap().containsKey(instrument))
      .collect(Collectors.toList());
    subscribe(MarketDataResponseType.TRADING_STATUS, filteredInstruments, requestBuilder)
      .whenComplete((subscriptionResult, __) -> Optional.ofNullable(subscriptionResult).ifPresent(result -> {
        context.getTradingStatusesSubscriptionsMap().putAll(result.getSubscriptionStatusMap());
        context.getOnTradingStatusListeners().add(onTradingStatusListener);
      }));
  }

  /**
   * Метод для проверки наличия подписки на свечи по инструменту
   *
   * @param instrument Инструмент
   * @return true если активная подписка есть, false если нет
   */
  public boolean isSubscribedCandles(Instrument instrument) {
    return checkInstrumentSubscription(context.getCandlesSubscriptionsMap(), instrument);
  }

  /**
   * Метод для проверки наличия подписки на последнюю цену по инструменту
   *
   * @param instrument Инструмент
   * @return true если активная подписка есть, false если нет
   */
  public boolean isSubscribedLastPrice(Instrument instrument) {
    return checkInstrumentSubscription(context.getLastPricesSubscriptionsMap(), instrument);
  }

  /**
   * Метод для проверки наличия подписки на сделки по инструменту
   *
   * @param instrument Инструмент
   * @return true если активная подписка есть, false если нет
   */
  public boolean isSubscribedTrades(Instrument instrument) {
    return checkInstrumentSubscription(context.getTradesSubscriptionsMap(), instrument);
  }

  /**
   * Метод для проверки наличия подписки на торговый стакан по инструменту
   *
   * @param instrument Инструмент
   * @return true если активная подписка есть, false если нет
   */
  public boolean isSubscribedOrderBook(Instrument instrument) {
    return checkInstrumentSubscription(context.getOrderBooksSubscriptionsMap(), instrument);
  }

  /**
   * Метод для проверки наличия подписки на статус торгов по инструменту
   *
   * @param instrument Инструмент
   * @return true если активная подписка есть, false если нет
   */
  public boolean isSubscribedTradingStatuses(Instrument instrument) {
    return checkInstrumentSubscription(context.getOrderBooksSubscriptionsMap(), instrument);
  }

  public StreamServiceStubFactory getStreamFactory() {
    return streamFactory;
  }

  public void shutdown() {
    streamWrappers.forEach(MarketDataStreamWrapper::disconnect);
  }

  private boolean checkInstrumentSubscription(
    Map<Instrument, SubscriptionStatus> subscriptionStatusMap,
    Instrument instrument
  ) {
    return Optional.ofNullable(subscriptionStatusMap.get(instrument))
      .map(SubscriptionStatus::isOk)
      .orElse(false);
  }

  private CompletableFuture<SubscriptionResult> subscribe(
    MarketDataResponseType responseType,
    List<Instrument> instruments,
    Function<List<Instrument>, MarketDataRequest> requestBuilder
  ) {
    if (instruments.isEmpty()) {
      return CompletableFuture.failedFuture(new IllegalArgumentException("Instruments list is empty"));
    }
    var supplier = Lazy.of(() -> CompletableFuture.supplyAsync(() -> {
      var subscriptionResults = new HashMap<Instrument, SubscriptionStatus>();
      int i = 0;
      while (i < instruments.size()) {
        var streamWrapper = getAvailableStreamWrapper();
        int endIndex = Math.min(
          instruments.size(),
          i + configuration.getMaxMarketDataSubscriptionsCount() - streamWrapper.getSubscriptionsCount()
        );
        var sublist = instruments.subList(i, endIndex);
        i = endIndex;
        var subscriptionResult = streamWrapper.subscribe(requestBuilder.apply(sublist), responseType, sublist);
        subscriptionResults.putAll(subscriptionResult.getSubscriptionStatusMap());
      }
      return new SubscriptionResult(responseType, subscriptionResults);
    }));
    return lastTask.updateAndGet(previousTask -> previousTask.thenCompose(previousResult -> supplier.get()));
  }

  private MarketDataStreamWrapper getAvailableStreamWrapper() {
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

  private MarketDataStreamWrapper createStreamWrapper() {
    return new MarketDataStreamWrapper(
      streamFactory,
      context.getGlobalOnCandleListener(),
      context.getGlobalOnLastPriceListener()
    );
  }

  private <T extends ResponseWrapper<?>> void startListenersProcessing(
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
