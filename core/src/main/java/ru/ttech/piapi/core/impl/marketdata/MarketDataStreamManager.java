package ru.ttech.piapi.core.impl.marketdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;
import ru.ttech.piapi.core.impl.marketdata.subscription.SubscriptionResult;
import ru.ttech.piapi.core.impl.marketdata.subscription.SubscriptionStatus;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.LastPriceWrapper;
import ru.ttech.piapi.core.impl.wrapper.ResponseWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

public class MarketDataStreamManager {

  private static final Logger logger = LoggerFactory.getLogger(MarketDataStreamManager.class);

  private final StreamServiceStubFactory streamFactory;
  private final ConnectorConfiguration configuration;
  private final OnNextListener<CandleWrapper> globalOnCandleListener;
  private final OnNextListener<LastPriceWrapper> globalLastPriceListener;
  private final List<OnNextListener<CandleWrapper>> onCandleListeners = Collections.synchronizedList(new ArrayList<>());
  private final List<OnNextListener<LastPriceWrapper>> onLastPriceListeners = Collections.synchronizedList(new ArrayList<>());
  private final List<MarketDataStreamWrapper> streamWrappers = Collections.synchronizedList(new ArrayList<>());
  private final BlockingQueue<CandleWrapper> candleQueue = new LinkedBlockingQueue<>();
  private final BlockingQueue<LastPriceWrapper> lastPriceQueue = new LinkedBlockingQueue<>();

  public MarketDataStreamManager(StreamServiceStubFactory streamFactory, ExecutorService executorService) {
    this.streamFactory = streamFactory;
    this.configuration = streamFactory.getServiceStubFactory().getConfiguration();
    this.globalOnCandleListener = candleQueue::offer;
    this.globalLastPriceListener = lastPriceQueue::offer;
    executorService.submit(() -> startListenersProcessing(candleQueue, onCandleListeners));
    executorService.submit(() -> startListenersProcessing(lastPriceQueue, onLastPriceListeners));
  }

  public synchronized SubscriptionResult subscribeCandles(
    List<Instrument> instruments,
    GetCandlesRequest.CandleSource candleSource,
    OnNextListener<CandleWrapper> onCandleListener
  ) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = instrumentsSublist ->
      MarketDataRequestBuilder.buildCandlesRequest(instrumentsSublist, candleSource);
    return subscribe(MarketDataResponseType.CANDLE, instruments, requestBuilder)
      .whenCompleteAsync((subscriptionResult, throwable) -> onCandleListeners.add(onCandleListener)).join();
  }

  public synchronized SubscriptionResult subscribeLastPrices(
    List<Instrument> instruments,
    OnNextListener<LastPriceWrapper> onLastPriceListener
  ) {
    Function<List<Instrument>, MarketDataRequest> requestBuilder = MarketDataRequestBuilder::buildLastPricesRequest;
    return subscribe(MarketDataResponseType.LAST_PRICE, instruments, requestBuilder)
      .whenCompleteAsync((subscriptionResult, throwable) -> onLastPriceListeners.add(onLastPriceListener)).join();
  }

  public StreamServiceStubFactory getStreamFactory() {
    return streamFactory;
  }

  public void shutdown() {
    streamWrappers.forEach(MarketDataStreamWrapper::disconnect);
  }

  protected CompletableFuture<SubscriptionResult> subscribe(
    MarketDataResponseType responseType,
    List<Instrument> instruments,
    Function<List<Instrument>, MarketDataRequest> requestBuilder
  ) {
    if (instruments.isEmpty()) {
      throw new IllegalArgumentException("Instruments list is empty");
    }
    return CompletableFuture.supplyAsync(() -> {
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
    });
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
    return new MarketDataStreamWrapper(streamFactory, globalOnCandleListener, globalLastPriceListener);
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
