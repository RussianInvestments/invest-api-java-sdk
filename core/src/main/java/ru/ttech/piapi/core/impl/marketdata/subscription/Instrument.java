package ru.ttech.piapi.core.impl.marketdata.subscription;

import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;

import java.util.Objects;

public class Instrument {

  private final String instrumentUid;
  private SubscriptionInterval subscriptionInterval;

  public Instrument(String instrumentUid) {
    this.instrumentUid = instrumentUid;
  }

  public Instrument(String instrumentUid, SubscriptionInterval subscriptionInterval) {
    this(instrumentUid);
    this.subscriptionInterval = subscriptionInterval;
  }

  public String getInstrumentUid() {
    return instrumentUid;
  }

  public SubscriptionInterval getSubscriptionInterval() {
    return subscriptionInterval;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Instrument that = (Instrument) o;
    return instrumentUid.equals(that.instrumentUid) && subscriptionInterval == that.subscriptionInterval;
  }

  @Override
  public int hashCode() {
    return Objects.hash(instrumentUid, subscriptionInterval);
  }
}
