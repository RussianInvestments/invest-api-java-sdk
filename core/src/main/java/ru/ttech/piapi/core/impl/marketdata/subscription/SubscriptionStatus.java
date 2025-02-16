package ru.ttech.piapi.core.impl.marketdata.subscription;

public enum SubscriptionStatus {
  OK,
  ERROR;

  public boolean isOk() {
    return this == OK;
  }

  public boolean isError() {
    return this == ERROR;
  }
}
