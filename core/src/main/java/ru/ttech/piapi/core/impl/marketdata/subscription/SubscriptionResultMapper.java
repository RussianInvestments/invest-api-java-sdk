package ru.ttech.piapi.core.impl.marketdata.subscription;

import ru.tinkoff.piapi.contract.v1.SubscribeCandlesResponse;
import ru.tinkoff.piapi.contract.v1.SubscribeInfoResponse;
import ru.tinkoff.piapi.contract.v1.SubscribeLastPriceResponse;
import ru.tinkoff.piapi.contract.v1.SubscribeOrderBookResponse;
import ru.tinkoff.piapi.contract.v1.SubscribeTradesResponse;
import ru.ttech.piapi.core.impl.marketdata.MarketDataResponseType;

import java.util.stream.Collectors;

public class SubscriptionResultMapper {

  public static MarketDataSubscriptionResult map(SubscribeCandlesResponse subscribeCandlesResponse) {
    return new MarketDataSubscriptionResult(
      MarketDataResponseType.CANDLE,
      subscribeCandlesResponse.getCandlesSubscriptionsList().stream()
        .collect(Collectors.toMap(
          candleSubscription -> new Instrument(candleSubscription.getInstrumentUid(), candleSubscription.getInterval()),
          candleSubscription -> mapSubscriptionStatus(candleSubscription.getSubscriptionStatus()))
        )
    );
  }

  public static MarketDataSubscriptionResult map(SubscribeLastPriceResponse subscribeLastPriceResponse) {
    return new MarketDataSubscriptionResult(
      MarketDataResponseType.LAST_PRICE,
      subscribeLastPriceResponse.getLastPriceSubscriptionsList().stream()
        .collect(Collectors.toMap(
          subscription -> new Instrument(subscription.getInstrumentUid()),
          subscription -> mapSubscriptionStatus(subscription.getSubscriptionStatus()))
        )
    );
  }

  public static MarketDataSubscriptionResult map(SubscribeOrderBookResponse subscribeOrderBookResponse) {
    return new MarketDataSubscriptionResult(
      MarketDataResponseType.ORDER_BOOK,
      subscribeOrderBookResponse.getOrderBookSubscriptionsList().stream()
        .collect(Collectors.toMap(
          subscription -> new Instrument(
            subscription.getInstrumentUid(), subscription.getDepth(), subscription.getOrderBookType()
          ),
          subscription -> mapSubscriptionStatus(subscription.getSubscriptionStatus()))
        )
    );
  }

  public static MarketDataSubscriptionResult map(SubscribeTradesResponse subscribeTradesResponse) {
    return new MarketDataSubscriptionResult(
      MarketDataResponseType.TRADE,
      subscribeTradesResponse.getTradeSubscriptionsList().stream()
        .collect(Collectors.toMap(
          subscription -> new Instrument(subscription.getInstrumentUid()),
          subscription -> mapSubscriptionStatus(subscription.getSubscriptionStatus()))
        )
    );
  }

  public static MarketDataSubscriptionResult map(SubscribeInfoResponse subscribeInfoResponse) {
    return new MarketDataSubscriptionResult(
      MarketDataResponseType.TRADING_STATUS,
      subscribeInfoResponse.getInfoSubscriptionsList().stream()
        .collect(Collectors.toMap(
          subscription -> new Instrument(subscription.getInstrumentUid()),
          subscription -> mapSubscriptionStatus(subscription.getSubscriptionStatus()))
        )
    );
  }

  private static SubscriptionStatus mapSubscriptionStatus(
    ru.tinkoff.piapi.contract.v1.SubscriptionStatus subscriptionStatus
  ) {
    return subscriptionStatus == ru.tinkoff.piapi.contract.v1.SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS
      ? SubscriptionStatus.OK
      : SubscriptionStatus.ERROR;
  }
}
