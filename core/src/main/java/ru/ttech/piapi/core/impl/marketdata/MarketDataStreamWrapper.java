package ru.ttech.piapi.core.impl.marketdata;

import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.contract.v1.MarketDataStreamServiceGrpc;
import ru.ttech.piapi.core.connector.streaming.BidirectionalStreamWrapper;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;
import ru.ttech.piapi.core.impl.marketdata.subscription.Subscription;
import ru.ttech.piapi.core.impl.marketdata.subscription.SubscriptionMapper;
import ru.ttech.piapi.core.impl.marketdata.subscription.SubscriptionStatus;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MarketDataStreamWrapper {

  private final SynchronousQueue<Subscription> subscriptions = new SynchronousQueue<>();
  private final AtomicInteger subscriptionsCount = new AtomicInteger(0);
  private final BidirectionalStreamWrapper<
    MarketDataStreamServiceGrpc.MarketDataStreamServiceStub,
    MarketDataRequest,
    MarketDataResponse> streamWrapper;

  public MarketDataStreamWrapper(
    StreamServiceStubFactory streamFactory,
    OnNextListener<CandleWrapper> globalOnCandleListener
  ) {
    this.streamWrapper = streamFactory.newBidirectionalStream(
      MarketDataStreamConfiguration.builder()
        .addOnCandleListener(globalOnCandleListener)
        .addOnNextListener(this::processSubscriptionResponse)
        .build());
    this.streamWrapper.connect();
  }

  public Map<Instrument, SubscriptionStatus> waitSubscriptionResult(
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

  public void subscribe(MarketDataRequest request) {
    streamWrapper.newCall(request);
  }

  public int getSubscriptionsCount() {
    return subscriptionsCount.get();
  }

  public void disconnect() {
    streamWrapper.disconnect();
  }

  private void processSubscriptionResponse(MarketDataResponse response) {
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

  private void updateSubscriptionsCount(Subscription subscription) {
    long successfulSubscriptionsCount = subscription.getSubscriptionStatusMap().values().stream()
      .filter(SubscriptionStatus::isOk).count();
    subscriptionsCount.addAndGet((int) successfulSubscriptionsCount);
  }
}
