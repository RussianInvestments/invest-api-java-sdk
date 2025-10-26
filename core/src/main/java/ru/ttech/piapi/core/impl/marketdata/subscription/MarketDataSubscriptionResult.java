package ru.ttech.piapi.core.impl.marketdata.subscription;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import ru.ttech.piapi.core.impl.marketdata.MarketDataResponseType;

import java.util.Map;

@Getter
@ToString
@RequiredArgsConstructor
public class MarketDataSubscriptionResult {

  private final RequestAction subscriptionAction;
  private final MarketDataResponseType responseType;
  private final Map<Instrument, SubscriptionStatus> subscriptionStatusMap;
}
