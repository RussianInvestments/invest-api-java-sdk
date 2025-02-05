package ru.ttech.piapi.strategy.candle;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.Strategy;
import org.ta4j.core.num.DecimalNum;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;

import java.util.function.Function;

public class CandleStrategyConfiguration {

  private final CandleInstrument instrument;
  private final GetCandlesRequest.CandleSource candleSource;
  private final int warmupLength;
  private final Strategy strategy;
  private final BarSeries barSeries;

  private CandleStrategyConfiguration(
    CandleInstrument instrument,
    GetCandlesRequest.CandleSource candleSource,
    int warmupLength,
    BarSeries barSeries,
    Strategy strategy
  ) {
    this.instrument = instrument;
    this.candleSource = candleSource;
    this.warmupLength = warmupLength;
    this.barSeries = barSeries;
    this.strategy = strategy;
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

  public BarSeries getBarSeries() {
    return barSeries;
  }

  public Strategy getStrategy() {
    return strategy;
  }

  public int getWarmupLength() {
    return warmupLength;
  }

  public static class Builder {

    private CandleInstrument instrument;
    private GetCandlesRequest.CandleSource candleSource;
    private int warmupLength;
    private BarSeries barSeries;
    private Strategy strategy;

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

    public Builder setStrategy(Function<BarSeries, Strategy> strategyConstructor) {
      this.barSeries = new BaseBarSeriesBuilder().withNumTypeOf(DecimalNum.class).build();
      this.strategy = strategyConstructor.apply(barSeries);
      return this;
    }

    public CandleStrategyConfiguration build() {
      return new CandleStrategyConfiguration(instrument, candleSource, warmupLength, barSeries, strategy);
    }
  }
}
