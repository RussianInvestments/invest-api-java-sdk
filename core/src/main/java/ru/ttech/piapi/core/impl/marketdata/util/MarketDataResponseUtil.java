package ru.ttech.piapi.core.impl.marketdata.util;

import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.ttech.piapi.core.impl.marketdata.subscription.MarketDataSubscriptionResult;
import ru.ttech.piapi.core.impl.marketdata.subscription.SubscriptionResultMapper;

import java.util.Optional;

public class MarketDataResponseUtil {

  public static Optional<MarketDataSubscriptionResult> getSubscriptionStatusFromResponse(MarketDataResponse response) {
    if (response.hasSubscribeCandlesResponse()) {
      return Optional.of(SubscriptionResultMapper.map(response.getSubscribeCandlesResponse()));
    } else if (response.hasSubscribeLastPriceResponse()) {
      return Optional.of(SubscriptionResultMapper.map(response.getSubscribeLastPriceResponse()));
    } else if (response.hasSubscribeOrderBookResponse()) {
      return Optional.of(SubscriptionResultMapper.map(response.getSubscribeOrderBookResponse()));
    } else if (response.hasSubscribeTradesResponse()) {
      return Optional.of(SubscriptionResultMapper.map(response.getSubscribeTradesResponse()));
    } else if (response.hasSubscribeInfoResponse()) {
      return Optional.of(SubscriptionResultMapper.map(response.getSubscribeInfoResponse()));
    }
    return Optional.empty();
  }
}
