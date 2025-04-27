package ru.tinkoff.piapi.core.models;

import ru.tinkoff.piapi.contract.v1.PositionsSecurities;

import javax.annotation.Nonnull;
import java.util.Objects;

@Deprecated(since = "1.30", forRemoval = true)
public class SecurityPosition {
  private final String figi;
  private final long blocked;
  private final long balance;
  private final String positionUid;
  private final String instrumentUid;
  private final boolean exchangeBlocked;
  private final String instrumentType;

  private SecurityPosition(@Nonnull String figi, long blocked, long balance,
                           @Nonnull String positionUid, @Nonnull String instrumentUid,
                           boolean exchangeBlocked, @Nonnull String instrumentType) {
    this.figi = figi;
    this.blocked = blocked;
    this.balance = balance;
    this.positionUid = positionUid;
    this.instrumentUid = instrumentUid;
    this.exchangeBlocked = exchangeBlocked;
    this.instrumentType = instrumentType;
  }

  @Nonnull
  public static SecurityPosition fromResponse(@Nonnull PositionsSecurities positionsSecurities) {
    return new SecurityPosition(
      positionsSecurities.getFigi(),
      positionsSecurities.getBlocked(),
      positionsSecurities.getBalance(),
      positionsSecurities.getPositionUid(),
      positionsSecurities.getInstrumentUid(),
      positionsSecurities.getExchangeBlocked(),
      positionsSecurities.getInstrumentType()
    );
  }

  @Nonnull
  public String getFigi() {
    return figi;
  }

  public long getBlocked() {
    return blocked;
  }

  public long getBalance() {
    return balance;
  }

  @Nonnull
  public String getPositionUid() {
    return positionUid;
  }

  @Nonnull
  public String getInstrumentUid() {
    return instrumentUid;
  }

  public boolean isExchangeBlocked() {
    return exchangeBlocked;
  }

  @Nonnull
  public String getInstrumentType() {
    return instrumentType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SecurityPosition that = (SecurityPosition) o;
    return blocked == that.blocked && balance == that.balance && figi.equals(that.figi)
      && positionUid.equals(that.positionUid) && instrumentUid.equals(that.instrumentUid)
      && exchangeBlocked == that.exchangeBlocked && instrumentType.equals(that.instrumentType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(figi, blocked, balance, positionUid, instrumentUid, exchangeBlocked);
  }
}
