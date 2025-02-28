package ru.ttech.piapi.core.impl.marketdata;

import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.contract.v1.MarketDataStreamServiceGrpc;
import ru.ttech.piapi.core.connector.streaming.BidirectionalStreamWrapper;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;
import ru.ttech.piapi.core.impl.marketdata.subscription.MarketDataSubscriptionResult;
import ru.ttech.piapi.core.impl.marketdata.subscription.SubscriptionResultMapper;
import ru.ttech.piapi.core.impl.marketdata.subscription.SubscriptionStatus;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.LastPriceWrapper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MarketDataStreamWrapper {

  protected final SynchronousQueue<MarketDataSubscriptionResult> subscriptionResultsQueue = new SynchronousQueue<>();
  protected final AtomicInteger subscriptionsCount = new AtomicInteger(0);
  protected final BidirectionalStreamWrapper<
    MarketDataStreamServiceGrpc.MarketDataStreamServiceStub,
    MarketDataRequest,
    MarketDataResponse> streamWrapper;

  public MarketDataStreamWrapper(
    StreamServiceStubFactory streamFactory,
    OnNextListener<CandleWrapper> globalOnCandleListener,
    OnNextListener<LastPriceWrapper> globalOnLastPriceListener
  ) {
    this.streamWrapper = streamFactory.newBidirectionalStream(
      MarketDataStreamConfiguration.builder()
        .addOnCandleListener(globalOnCandleListener)
        .addOnLastPriceListener(globalOnLastPriceListener)
        .addOnNextListener(this::processSubscriptionResponse)
        .build());
    this.streamWrapper.connect();
  }

  public int getSubscriptionsCount() {
    return subscriptionsCount.get();
  }

  public void disconnect() {
    streamWrapper.disconnect();
    subscriptionsCount.set(0);
  }

  public synchronized MarketDataSubscriptionResult subscribe(
    MarketDataRequest request,
    MarketDataResponseType responseType,
    List<Instrument> instruments
  ) {
    subscriptionResultsQueue.clear();
    streamWrapper.newCall(request);
    return waitSubscriptionResult(responseType, instruments);
  }

  protected MarketDataSubscriptionResult waitSubscriptionResult(
    MarketDataResponseType responseType,
    List<Instrument> instruments
  ) {
    try {
      var subscriptionResult = subscriptionResultsQueue.take();
      if (subscriptionResult.getResponseType() != responseType
        || !subscriptionResult.getSubscriptionStatusMap().keySet().containsAll(instruments)
      ) {
        throw new IllegalStateException("Wrong subscription result!");
      }
      return subscriptionResult;
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while waiting for subscription response", e);
    }
  }

  protected void processSubscriptionResponse(MarketDataResponse response) {
    Optional<MarketDataSubscriptionResult> subscription = Optional.empty();
    if (response.hasSubscribeCandlesResponse()) {
      subscription = Optional.of(SubscriptionResultMapper.map(response.getSubscribeCandlesResponse()));
    } else if (response.hasSubscribeLastPriceResponse()) {
      subscription = Optional.of(SubscriptionResultMapper.map(response.getSubscribeLastPriceResponse()));
    }
    if (subscription.isEmpty()) {
      return;
    }
    try {
      updateSubscriptionsCount(subscription.get());
      subscriptionResultsQueue.put(subscription.get());
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  protected void updateSubscriptionsCount(MarketDataSubscriptionResult subscriptionResult) {
    long successfulSubscriptionsCount = subscriptionResult.getSubscriptionStatusMap().values().stream()
      .filter(SubscriptionStatus::isOk).count();
    subscriptionsCount.addAndGet((int) successfulSubscriptionsCount);
  }
}
