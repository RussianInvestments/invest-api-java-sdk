package ru.ttech.piapi.core.impl.marketdata.subscription;

import ru.ttech.piapi.core.impl.marketdata.MarketDataResponseType;

import java.util.Map;

public class Subscription {

  private final MarketDataResponseType responseType;
  private final Map<Instrument, SubscriptionStatus> subscriptionStatusMap;

  public Subscription(
    MarketDataResponseType responseType,
    Map<Instrument, SubscriptionStatus> subscriptionStatusMap
  ) {
    this.responseType = responseType;
    this.subscriptionStatusMap = subscriptionStatusMap;
  }

  public MarketDataResponseType getResponseType() {
    return responseType;
  }

  public Map<Instrument, SubscriptionStatus> getSubscriptionStatusMap() {
    return subscriptionStatusMap;
  }
}
