package ru.ttech.piapi.core.impl.marketdata;

import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;
import ru.tinkoff.piapi.contract.v1.LastPriceInstrument;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.SubscribeCandlesRequest;
import ru.tinkoff.piapi.contract.v1.SubscribeLastPriceRequest;
import ru.tinkoff.piapi.contract.v1.SubscriptionAction;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;

import java.util.List;
import java.util.stream.Collectors;

public class MarketDataRequestBuilder {

  public static MarketDataRequest buildCandlesRequest(
    List<Instrument> instruments,
    GetCandlesRequest.CandleSource candleSource
  ) {
    return MarketDataRequest.newBuilder()
      .setSubscribeCandlesRequest(
        SubscribeCandlesRequest.newBuilder()
          .setSubscriptionAction(SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE)
          .addAllInstruments(instruments.stream()
            .map(instrument ->
              CandleInstrument.newBuilder()
                .setInstrumentId(instrument.getInstrumentUid())
                .setInterval(instrument.getSubscriptionInterval())
                .build())
            .collect(Collectors.toList()))
          .setWaitingClose(true)
          .setCandleSourceType(candleSource)
          .build())
      .build();
  }

  public static MarketDataRequest buildLastPricesRequest(List<Instrument> instruments) {
    return MarketDataRequest.newBuilder()
      .setSubscribeLastPriceRequest(SubscribeLastPriceRequest.newBuilder()
        .setSubscriptionAction(SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE)
        .addAllInstruments(instruments.stream()
          .map(instrument -> LastPriceInstrument.newBuilder().setInstrumentId(instrument.getInstrumentUid()).build())
          .collect(Collectors.toList()))
        .build())
      .build();
  }
}
