package ru.ttech.piapi.example.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.SubscribeCandlesRequest;
import ru.tinkoff.piapi.contract.v1.SubscriptionAction;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.impl.marketdata.MarketDataStreamWrapperConfiguration;

import java.util.concurrent.Executors;

public class MarketDataStreamWrapperExample {

  private static final Logger logger = LoggerFactory.getLogger(MarketDataStreamWrapperExample.class);

  public static void main(String[] args) {
    var configuration = ConnectorConfiguration.loadFromPropertiesFile("invest.properties");
    var factory = ServiceStubFactory.create(configuration);
    var streamFactory = StreamServiceStubFactory.create(factory);
    var executorService = Executors.newSingleThreadScheduledExecutor();
    var wrapper = streamFactory.newResilienceMarketDataStream(
      MarketDataStreamWrapperConfiguration.builder(executorService)
        .addOnCandleListener(response -> logger.info("New candle for instrument: {}", response.getInstrumentUid()))
        .build());
    var request = MarketDataRequest.newBuilder()
      .setSubscribeCandlesRequest(SubscribeCandlesRequest.newBuilder()
        .setSubscriptionAction(SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE)
        .addInstruments(CandleInstrument.newBuilder()
          .setInstrumentId("9978b56f-782a-4a80-a4b1-a48cbecfd194")
          .setInterval(SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE)
          .build())
        .setWaitingClose(true)
        .build())
      .build();
    wrapper.newCall(request);
    try {
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
