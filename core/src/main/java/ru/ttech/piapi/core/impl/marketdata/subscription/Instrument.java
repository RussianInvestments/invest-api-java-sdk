package ru.ttech.piapi.core.impl.marketdata.subscription;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;

@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class Instrument {

  private final String instrumentUid;
  private SubscriptionInterval subscriptionInterval;

  public Instrument(String instrumentUid, SubscriptionInterval subscriptionInterval) {
    this(instrumentUid);
    this.subscriptionInterval = subscriptionInterval;
  }
}
