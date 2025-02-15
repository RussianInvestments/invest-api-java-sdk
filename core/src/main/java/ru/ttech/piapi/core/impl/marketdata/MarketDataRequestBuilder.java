package ru.ttech.piapi.core.impl.marketdata;

import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.SubscribeCandlesRequest;
import ru.tinkoff.piapi.contract.v1.SubscriptionAction;

import java.util.List;

public class MarketDataRequestBuilder {

  public static MarketDataRequest buildCandlesRequest(
    List<CandleInstrument> instruments,
    GetCandlesRequest.CandleSource candleSource
  ) {
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
}
