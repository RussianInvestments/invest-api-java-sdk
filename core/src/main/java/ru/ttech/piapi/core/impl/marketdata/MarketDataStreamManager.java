package ru.ttech.piapi.core.impl.marketdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.contract.v1.MarketDataStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.SubscribeCandlesRequest;
import ru.tinkoff.piapi.contract.v1.SubscriptionAction;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.streaming.BidirectionalStreamWrapper;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;
import ru.ttech.piapi.core.impl.marketdata.subscription.Subscription;
import ru.ttech.piapi.core.impl.marketdata.subscription.SubscriptionMapper;
import ru.ttech.piapi.core.impl.marketdata.subscription.SubscriptionStatus;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MarketDataStreamManager {

  private static final Logger logger = LoggerFactory.getLogger(MarketDataStreamManager.class);

  private final StreamServiceStubFactory streamFactory;
  private final ConnectorConfiguration configuration;
  private final OnNextListener<CandleWrapper> globalOnCandleListener;
  private final List<OnNextListener<CandleWrapper>> onCandleListeners = new ArrayList<>();
  private final List<StreamTuple> streamWrappers = new ArrayList<>();

  public MarketDataStreamManager(StreamServiceStubFactory streamFactory, ExecutorService executorService) {
    this.streamFactory = streamFactory;
    this.configuration = streamFactory.getServiceStubFactory().getConfiguration();
    this.globalOnCandleListener = candle -> onCandleListeners.forEach(listener ->
      executorService.submit(() -> listener.onNext(candle)));
  }

  public void subscribeCandles(
    List<CandleInstrument> instruments,
    GetCandlesRequest.CandleSource candleSource,
    OnNextListener<CandleWrapper> onCandleListener
  ) {
    if (instruments.isEmpty()) {
      return;
    }
    int i = 0;
    while (i < instruments.size()) {
      var optionalTuple = streamWrappers.stream()
        .filter(tuple -> tuple.getSubscriptionsCount() < configuration.getMaxMarketDataSubscriptionsCount())
        .findFirst();
      StreamTuple tuple;
      List<CandleInstrument> sublist;
      if (optionalTuple.isPresent()) {
        tuple = optionalTuple.get();
        int endIndex = Math.min(
          instruments.size() - i,
          i + configuration.getMaxMarketDataStreamsCount() - tuple.getSubscriptionsCount()
        );
        sublist = instruments.subList(i, endIndex);
        i = endIndex;
        var request = buildMarketDataRequest(sublist, candleSource);
        tuple.getStreamWrapper().newCall(request);
      } else {
        int endIndex = Math.min(instruments.size(), i + configuration.getMaxMarketDataSubscriptionsCount());
        sublist = instruments.subList(i, endIndex);
        i = endIndex;
        tuple = createStreamTuple();
        var request = buildMarketDataRequest(sublist, candleSource);
        tuple.getStreamWrapper().newCall(request);
        streamWrappers.add(tuple);
      }
      var subscriptionInstruments = sublist.stream()
        .map(candleInstrument -> new Instrument(candleInstrument.getInstrumentId(), candleInstrument.getInterval()))
        .collect(Collectors.toList());
      // TODO: аггрегировать весь список подписок и отправлять пользователю их результат
      tuple.waitSubscriptions(subscriptionInstruments, MarketDataResponseType.CANDLE);
    }
    onCandleListeners.add(onCandleListener);
  }

  private StreamTuple createStreamTuple() {
    var tuple = new StreamTuple();
    var newWrapper = streamFactory.newBidirectionalStream(
      MarketDataStreamConfiguration.builder()
        .addOnCandleListener(globalOnCandleListener)
        .addOnNextListener(tuple::processSubscriptionResponse)
        .build());
    newWrapper.connect();
    tuple.setStreamWrapper(newWrapper);
    return tuple;
  }

  private MarketDataRequest buildMarketDataRequest(
    List<CandleInstrument> instruments,
    GetCandlesRequest.CandleSource candleSource) {
    return MarketDataRequest.newBuilder()
      .setSubscribeCandlesRequest(
        SubscribeCandlesRequest.newBuilder()
          .setSubscriptionAction(SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE)
          .addAllInstruments(instruments)
          .setWaitingClose(true)
          .setCandleSourceType(candleSource)
          .build())
      .build();
  }

  public StreamServiceStubFactory getStreamFactory() {
    return streamFactory;
  }

  public void shutdown() {
    streamWrappers.forEach(tuple -> tuple.getStreamWrapper().disconnect());
  }

  public static class StreamTuple {

    private final SynchronousQueue<Subscription> subscriptions = new SynchronousQueue<>();
    private final AtomicInteger subscriptionsCount = new AtomicInteger(0);
    private BidirectionalStreamWrapper<
      MarketDataStreamServiceGrpc.MarketDataStreamServiceStub,
      MarketDataRequest,
      MarketDataResponse> streamWrapper;

    public BidirectionalStreamWrapper<
      MarketDataStreamServiceGrpc.MarketDataStreamServiceStub,
      MarketDataRequest,
      MarketDataResponse> getStreamWrapper() {
      return streamWrapper;
    }

    public void setStreamWrapper(
      BidirectionalStreamWrapper<
        MarketDataStreamServiceGrpc.MarketDataStreamServiceStub,
        MarketDataRequest,
        MarketDataResponse> streamWrapper
    ) {
      this.streamWrapper = streamWrapper;
    }

    public Map<Instrument, SubscriptionStatus> waitSubscriptions(
      List<Instrument> instruments,
      MarketDataResponseType responseType
    ) {
      while (true) {
        try {
          var response = subscriptions.take();
          if (response.getResponseType() != responseType
            || !response.getSubscriptionStatusMap().keySet().containsAll(instruments)
          ) {
            subscriptions.put(response);
            continue;
          }
          return response.getSubscriptionStatusMap();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException("Interrupted while waiting for subscription response", e);
        }
      }
    }

    public void processSubscriptionResponse(MarketDataResponse response) {
      Optional<Subscription> subscription = Optional.empty();
      if (response.hasSubscribeCandlesResponse()) {
       subscription = Optional.of(SubscriptionMapper.map(response.getSubscribeCandlesResponse()));
      } else if (response.hasSubscribeLastPriceResponse()) {
        subscription = Optional.of(SubscriptionMapper.map(response.getSubscribeLastPriceResponse()));
      }
      if (subscription.isEmpty()) {
        return;
      }
      try {
        updateSubscriptionsCount(subscription.get());
        subscriptions.put(subscription.get());
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    public int getSubscriptionsCount() {
      return subscriptionsCount.get();
    }

    public void updateSubscriptionsCount(Subscription subscription) {
      long successfulSubscriptionsCount = subscription.getSubscriptionStatusMap().values().stream()
        .filter(SubscriptionStatus::isOk).count();
      subscriptionsCount.addAndGet((int) successfulSubscriptionsCount);
    }
  }
}
