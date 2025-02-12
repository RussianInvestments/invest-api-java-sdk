package ru.ttech.piapi.strategy.candle.live;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.Strategy;
import org.ta4j.core.num.DecimalNum;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;

import java.util.function.Consumer;
import java.util.function.Function;

public class CandleStrategyConfiguration {

  private final CandleInstrument instrument;
  private final GetCandlesRequest.CandleSource candleSource;
  private final int warmupLength;
  private final Strategy strategy;
  private final BarSeries barSeries;
  private final Consumer<Bar> enterAction;
  private final Consumer<Bar> exitAction;

  private CandleStrategyConfiguration(
    CandleInstrument instrument,
    GetCandlesRequest.CandleSource candleSource,
    int warmupLength,
    BarSeries barSeries,
    Strategy strategy,
    Consumer<Bar> enterAction,
    Consumer<Bar> exitAction
  ) {
    this.instrument = instrument;
    this.candleSource = candleSource;
    this.warmupLength = warmupLength;
    this.barSeries = barSeries;
    this.strategy = strategy;
    this.enterAction = enterAction;
    this.exitAction = exitAction;
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

  public Consumer<Bar> getEnterAction() {
    return enterAction;
  }

  public Consumer<Bar> getExitAction() {
    return exitAction;
  }

  public static class Builder {

    private CandleInstrument instrument;
    private GetCandlesRequest.CandleSource candleSource;
    private int warmupLength;
    private Function<BarSeries, Strategy> strategyConstructor;
    private Consumer<Bar> enterAction;
    private Consumer<Bar> exitAction;

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
      this.strategyConstructor = strategyConstructor;
      return this;
    }

    public Builder setStrategyEnterAction(Consumer<Bar> entryAction) {
      this.enterAction = entryAction;
      return this;
    }

    public Builder setStrategyExitAction(Consumer<Bar> exitAction) {
      this.exitAction = exitAction;
      return this;
    }

    public CandleStrategyConfiguration build() {
      if (instrument == null) {
        throw new IllegalStateException("Instrument is not set");
      }
      if (strategyConstructor == null) {
        throw new IllegalStateException("Strategy constructor is not set");
      }
      var barSeries = new BaseBarSeriesBuilder()
        .withName(instrument.getInstrumentId())
        .withNumTypeOf(DecimalNum.class)
        .build();
      var strategy = strategyConstructor.apply(barSeries);
      return new CandleStrategyConfiguration(
        instrument, candleSource, warmupLength, barSeries, strategy, enterAction, exitAction
      );
    }
  }
}
