package ru.tinkoff.piapi.core.models;

import ru.tinkoff.piapi.contract.v1.PositionsFutures;

import javax.annotation.Nonnull;
import java.util.Objects;

public class FuturePosition {
  private final String figi;
  private final long blocked;
  private final long balance;
  private final String positionUid;
  private final String instrumentUid;

  private FuturePosition(@Nonnull String figi, long blocked, long balance,
                         @Nonnull String positionUid, @Nonnull String instrumentUid) {
    this.figi = figi;
    this.blocked = blocked;
    this.balance = balance;
    this.positionUid = positionUid;
    this.instrumentUid = instrumentUid;
  }

  @Nonnull
  public static FuturePosition fromResponse(@Nonnull PositionsFutures positionsFutures) {
    return new FuturePosition(
      positionsFutures.getFigi(),
      positionsFutures.getBlocked(),
      positionsFutures.getBalance(),
      positionsFutures.getPositionUid(),
      positionsFutures.getInstrumentUid()
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FuturePosition that = (FuturePosition) o;
    return blocked == that.blocked && balance == that.balance && figi.equals(that.figi)
      && positionUid.equals(that.positionUid) && instrumentUid.equals(that.instrumentUid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(figi, blocked, balance, positionUid, instrumentUid);
  }
}
