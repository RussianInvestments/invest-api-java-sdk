package ru.ttech.piapi.core.impl.marketdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;
import ru.ttech.piapi.core.impl.marketdata.subscription.Subscription;
import ru.ttech.piapi.core.impl.marketdata.subscription.SubscriptionStatus;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class MarketDataStreamManager {

  private static final Logger logger = LoggerFactory.getLogger(MarketDataStreamManager.class);

  private final StreamServiceStubFactory streamFactory;
  private final ConnectorConfiguration configuration;
  private final OnNextListener<CandleWrapper> globalOnCandleListener;
  private final List<OnNextListener<CandleWrapper>> onCandleListeners = new ArrayList<>();
  private final List<MarketDataStreamWrapper> streamWrappers = new ArrayList<>();

  public MarketDataStreamManager(StreamServiceStubFactory streamFactory, ExecutorService executorService) {
    this.streamFactory = streamFactory;
    this.configuration = streamFactory.getServiceStubFactory().getConfiguration();
    this.globalOnCandleListener = candle -> onCandleListeners.forEach(listener ->
      executorService.submit(() -> listener.onNext(candle)));
  }

  public Subscription subscribeCandles(
    List<CandleInstrument> instruments,
    GetCandlesRequest.CandleSource candleSource,
    OnNextListener<CandleWrapper> onCandleListener
  ) {
    if (instruments.isEmpty()) {
      throw new IllegalArgumentException("Instruments list is empty");
    }
    int i = 0;
    var subscriptions = new HashMap<Instrument, SubscriptionStatus>();
    while (i < instruments.size()) {
      var wrapperOptional = streamWrappers.stream()
        .filter(tuple -> tuple.getSubscriptionsCount() < configuration.getMaxMarketDataSubscriptionsCount())
        .findFirst();
      MarketDataStreamWrapper wrapper;
      List<CandleInstrument> sublist;
      if (wrapperOptional.isPresent()) {
        wrapper = wrapperOptional.get();
        int endIndex = Math.min(
          instruments.size(),
          i + configuration.getMaxMarketDataStreamsCount() - wrapper.getSubscriptionsCount()
        );
        sublist = instruments.subList(i, endIndex);
        i = endIndex;
        var request = MarketDataRequestBuilder.buildCandlesRequest(sublist, candleSource);
        wrapper.subscribe(request);
      } else {
        int endIndex = Math.min(instruments.size(), i + configuration.getMaxMarketDataSubscriptionsCount());
        sublist = instruments.subList(i, endIndex);
        i = endIndex;
        wrapper = createStreamTuple();
        var request = MarketDataRequestBuilder.buildCandlesRequest(sublist, candleSource);
        wrapper.subscribe(request);
        streamWrappers.add(wrapper);
      }
      var subscriptionInstruments = sublist.stream()
        .map(candleInstrument -> new Instrument(candleInstrument.getInstrumentId(), candleInstrument.getInterval()))
        .collect(Collectors.toList());
      var subscriptionResults = wrapper.waitSubscriptionResult(subscriptionInstruments, MarketDataResponseType.CANDLE);
      subscriptions.putAll(subscriptionResults);

    }
    onCandleListeners.add(onCandleListener);
    return new Subscription(MarketDataResponseType.CANDLE, subscriptions);
  }

  private MarketDataStreamWrapper createStreamTuple() {
    return new MarketDataStreamWrapper(streamFactory, globalOnCandleListener);
  }

  public StreamServiceStubFactory getStreamFactory() {
    return streamFactory;
  }

  public void shutdown() {
    streamWrappers.forEach(MarketDataStreamWrapper::disconnect);
  }
}
