package ru.ttech.piapi.core.impl.marketdata.util;

import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;
import ru.tinkoff.piapi.contract.v1.InfoInstrument;
import ru.tinkoff.piapi.contract.v1.LastPriceInstrument;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.OrderBookInstrument;
import ru.tinkoff.piapi.contract.v1.SubscribeCandlesRequest;
import ru.tinkoff.piapi.contract.v1.SubscribeInfoRequest;
import ru.tinkoff.piapi.contract.v1.SubscribeLastPriceRequest;
import ru.tinkoff.piapi.contract.v1.SubscribeOrderBookRequest;
import ru.tinkoff.piapi.contract.v1.SubscribeTradesRequest;
import ru.tinkoff.piapi.contract.v1.SubscriptionAction;
import ru.tinkoff.piapi.contract.v1.TradeInstrument;
import ru.tinkoff.piapi.contract.v1.TradeSourceType;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;

import java.util.List;
import java.util.stream.Collectors;

public class MarketDataRequestBuilder {

  public static MarketDataRequest buildCandlesRequest(
    List<Instrument> instruments,
    GetCandlesRequest.CandleSource candleSource,
    boolean waitingClose
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
          .setWaitingClose(waitingClose)
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

  public static MarketDataRequest buildOrderBooksRequest(List<Instrument> instruments) {
    return MarketDataRequest.newBuilder()
      .setSubscribeOrderBookRequest(SubscribeOrderBookRequest.newBuilder()
        .setSubscriptionAction(SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE)
        .addAllInstruments(instruments.stream()
          .map(instrument -> OrderBookInstrument.newBuilder()
            .setOrderBookType(instrument.getOrderBookType())
            .setDepth(instrument.getDepth())
            .setInstrumentId(instrument.getInstrumentUid())
            .build())
          .collect(Collectors.toList()))
        .build())
      .build();
  }

  public static MarketDataRequest buildTradesRequest(
    List<Instrument> instruments,
    TradeSourceType tradeSourceType
  ) {
    return MarketDataRequest.newBuilder()
      .setSubscribeTradesRequest(SubscribeTradesRequest.newBuilder()
        .setSubscriptionAction(SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE)
        .addAllInstruments(instruments.stream()
          .map(instrument -> TradeInstrument.newBuilder()
            .setInstrumentId(instrument.getInstrumentUid())
            .build())
          .collect(Collectors.toList()))
        .setTradeSource(tradeSourceType)
        .build())
      .build();
  }

  public static MarketDataRequest buildTradingStatusesRequest(List<Instrument> instruments) {
    return MarketDataRequest.newBuilder()
      .setSubscribeInfoRequest(SubscribeInfoRequest.newBuilder()
        .setSubscriptionAction(SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE)
        .addAllInstruments(instruments.stream()
          .map(instrument -> InfoInstrument.newBuilder().setInstrumentId(instrument.getInstrumentUid()).build())
          .collect(Collectors.toList()))
        .build())
      .build();
  }
}
