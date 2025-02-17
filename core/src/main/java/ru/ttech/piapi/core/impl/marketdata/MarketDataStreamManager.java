package ru.ttech.piapi.core.impl.marketdata;

import io.vavr.Lazy;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.TradeSourceType;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

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

  public void start() {
    executor.submit(() -> startListenersProcessing(context.getCandleQueue(), context.getOnCandleListeners()));
    executor.submit(() -> startListenersProcessing(context.getLastPriceQueue(), context.getOnLastPriceListeners()));
    executor.submit(() -> startListenersProcessing(context.getTradesQueue(), context.getOnTradeListeners()));
    executor.submit(() -> startListenersProcessing(context.getOrderBooksQueue(), context.getOnOrderBookListeners()));
    executor.submit(() -> startListenersProcessing(context.getTradingStatusesQueue(), context.getOnTradingStatusListeners()));
  }

  public void subscribeCandles(
    List<Instrument> instruments,
    GetCandlesRequest.CandleSource candleSource,
    boolean waitingClose,
    OnNextListener<CandleWrapper> onCandleListener
  ) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = instrumentsSublist ->
      MarketDataRequestBuilder.buildCandlesRequest(instrumentsSublist, candleSource, waitingClose);
    var filteredInstruments = instruments.stream()
      .filter(instrument -> context.getCandlesSubscriptionsMap().containsKey(instrument))
      .collect(Collectors.toList());
    subscribe(MarketDataResponseType.CANDLE, filteredInstruments, requestBuilder)
      .whenComplete((ignore, __) -> context.getOnCandleListeners().add(onCandleListener))
      .whenComplete((result, __) -> context.getCandlesSubscriptionsMap().putAll(result.getSubscriptionStatusMap()));
  }

  public void subscribeLastPrices(
    List<Instrument> instruments,
    OnNextListener<LastPriceWrapper> onLastPriceListener
  ) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = MarketDataRequestBuilder::buildLastPricesRequest;
    var filteredInstruments = instruments.stream()
      .filter(instrument -> context.getLastPricesSubscriptionsMap().containsKey(instrument))
      .collect(Collectors.toList());
    subscribe(MarketDataResponseType.LAST_PRICE, filteredInstruments, requestBuilder)
      .whenComplete((ignore, __) -> context.getOnLastPriceListeners().add(onLastPriceListener))
      .whenComplete((result, __) -> context.getLastPricesSubscriptionsMap().putAll(result.getSubscriptionStatusMap()));
  }

  public void subscribeTrades(
    List<Instrument> instruments,
    TradeSourceType tradeSourceType,
    OnNextListener<TradeWrapper> onTradeListener
  ) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = instrumentsSublist ->
      MarketDataRequestBuilder.buildTradesRequest(instrumentsSublist, tradeSourceType);
    var filteredInstruments = instruments.stream()
      .filter(instrument -> context.getTradesSubscriptionsMap().containsKey(instrument))
      .collect(Collectors.toList());
    subscribe(MarketDataResponseType.TRADE, filteredInstruments, requestBuilder)
      .whenComplete((ignore, __) -> context.getOnTradeListeners().add(onTradeListener))
      .whenComplete((result, __) -> context.getTradesSubscriptionsMap().putAll(result.getSubscriptionStatusMap()));
  }

  public void subscribeOrderBooks(
    List<Instrument> instruments,
    OnNextListener<OrderBookWrapper> onOrderBookListener
  ) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = MarketDataRequestBuilder::buildOrderBooksRequest;
    var filteredInstruments = instruments.stream()
      .filter(instrument -> context.getOrderBooksSubscriptionsMap().containsKey(instrument))
      .collect(Collectors.toList());
    subscribe(MarketDataResponseType.ORDER_BOOK, filteredInstruments, requestBuilder)
      .whenComplete((ignore, __) -> context.getOnOrderBookListeners().add(onOrderBookListener))
      .whenComplete((result, __) -> context.getOrderBooksSubscriptionsMap().putAll(result.getSubscriptionStatusMap()));
  }

  public void subscribeTradingStatuses(
    List<Instrument> instruments,
    OnNextListener<TradingStatusWrapper> onTradingStatusListener
  ) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = MarketDataRequestBuilder::buildTradingStatusesRequest;
    var filteredInstruments = instruments.stream()
      .filter(instrument -> context.getTradingStatusesSubscriptionsMap().containsKey(instrument))
      .collect(Collectors.toList());
    subscribe(MarketDataResponseType.TRADING_STATUS, filteredInstruments, requestBuilder)
      .whenComplete((ignore, __) -> context.getOnTradingStatusListeners().add(onTradingStatusListener))
      .whenComplete((result, __) -> context.getTradingStatusesSubscriptionsMap().putAll(result.getSubscriptionStatusMap()));
  }

  public boolean isSubscribedCandles(Instrument instrument) {
    return checkInstrumentSubscription(context.getCandlesSubscriptionsMap(), instrument);
  }

  public boolean isSubscribedLastPrice(Instrument instrument) {
    return checkInstrumentSubscription(context.getLastPricesSubscriptionsMap(), instrument);
  }

  public boolean isSubscribedTrades(Instrument instrument) {
    return checkInstrumentSubscription(context.getTradesSubscriptionsMap(), instrument);
  }

  public boolean isSubscribedOrderBook(Instrument instrument) {
    return checkInstrumentSubscription(context.getOrderBooksSubscriptionsMap(), instrument);
  }

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

  protected CompletableFuture<SubscriptionResult> subscribe(
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
