package ru.tinkoff.piapi.core.models;

import ru.tinkoff.piapi.contract.v1.PositionsOptions;

import javax.annotation.Nonnull;
import java.util.Objects;

@Deprecated(since = "1.30", forRemoval = true)
public class OptionPosition {
  private final long blocked;
  private final long balance;
  private final String positionUid;
  private final String instrumentUid;

  private OptionPosition(long blocked, long balance, @Nonnull String positionUid, @Nonnull String instrumentUid) {
    this.blocked = blocked;
    this.balance = balance;
    this.positionUid = positionUid;
    this.instrumentUid = instrumentUid;
  }

  public static OptionPosition fromResponse(@Nonnull PositionsOptions positionsOptions) {
    return new OptionPosition(
      positionsOptions.getBlocked(),
      positionsOptions.getBalance(),
      positionsOptions.getPositionUid(),
      positionsOptions.getInstrumentUid()
    );
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
    OptionPosition that = (OptionPosition) o;
    return blocked == that.blocked && balance == that.balance && positionUid.equals(that.positionUid)
      && instrumentUid.equals(that.instrumentUid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(blocked, balance, positionUid, instrumentUid);
  }
}
