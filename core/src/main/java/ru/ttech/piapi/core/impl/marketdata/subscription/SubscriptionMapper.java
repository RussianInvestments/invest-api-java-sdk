package ru.ttech.piapi.core.impl.marketdata.subscription;

import ru.tinkoff.piapi.contract.v1.SubscribeCandlesResponse;
import ru.tinkoff.piapi.contract.v1.SubscribeLastPriceResponse;
import ru.ttech.piapi.core.impl.marketdata.MarketDataResponseType;

import java.util.stream.Collectors;

public class SubscriptionMapper {

  public static Subscription map(SubscribeCandlesResponse subscribeCandlesResponse) {
    return new Subscription(
      MarketDataResponseType.CANDLE,
      subscribeCandlesResponse.getCandlesSubscriptionsList().stream()
        .collect(Collectors.toMap(
          candleSubscription -> new Instrument(candleSubscription.getInstrumentUid(), candleSubscription.getInterval()),
          candleSubscription -> mapSubscriptionStatus(candleSubscription.getSubscriptionStatus()))
        )
    );
  }

  public static Subscription map(SubscribeLastPriceResponse subscribeLastPriceResponse) {
    return new Subscription(
      MarketDataResponseType.LAST_PRICE,
      subscribeLastPriceResponse.getLastPriceSubscriptionsList().stream()
        .collect(Collectors.toMap(
          lastPriceSubscription -> new Instrument(lastPriceSubscription.getInstrumentUid()),
          lastPriceSubscription -> mapSubscriptionStatus(lastPriceSubscription.getSubscriptionStatus()))
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
