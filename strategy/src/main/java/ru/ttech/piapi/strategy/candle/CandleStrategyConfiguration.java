package ru.ttech.piapi.strategy.candle;

import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;

public class CandleStrategyConfiguration {

  private final CandleInstrument instrument;
  private final GetCandlesRequest.CandleSource candleSource;
  private final int warmupLength;

  private CandleStrategyConfiguration(
    CandleInstrument instrument,
    GetCandlesRequest.CandleSource candleSource,
    int warmupLength
  ) {
    this.instrument = instrument;
    this.candleSource = candleSource;
    this.warmupLength = warmupLength;
  }

  public static Builder builder() {
    return new Builder();
  }

  public CandleInstrument getInstrument() {
    return instrument;
  }

  public GetCandlesRequest.CandleSource getCandleSource() {
    return candleSource;
  }

  public int getWarmupLength() {
    return warmupLength;
  }

  public static class Builder {

    private CandleInstrument instrument;
    private GetCandlesRequest.CandleSource candleSource;
    private int warmupLength;

    public Builder setInstrument(CandleInstrument instrument) {
      this.instrument = instrument;
      return this;
    }

    public Builder setCandleSource(GetCandlesRequest.CandleSource candleSource) {
      this.candleSource = candleSource;
      return this;
    }

    public Builder setWarmupLength(int warmupLength) {
      this.warmupLength = warmupLength;
      return this;
    }

    public CandleStrategyConfiguration build() {
      return new CandleStrategyConfiguration(instrument, candleSource, warmupLength);
    }
  }
}
