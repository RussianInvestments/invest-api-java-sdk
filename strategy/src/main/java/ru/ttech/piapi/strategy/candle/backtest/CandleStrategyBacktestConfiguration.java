package ru.ttech.piapi.strategy.candle.backtest;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.DecimalNum;
import ru.tinkoff.piapi.contract.v1.CandleInterval;

import java.time.LocalDate;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class CandleStrategyBacktestConfiguration {

  private final String instrumentId;
  private final CandleInterval candleInterval;
  private final BarSeries barSeries;
  private final Strategy strategy;
  private final LocalDate from;
  private final LocalDate to;
  private final BiConsumer<BarSeries, TradingRecord> strategyAnalysis;

  private CandleStrategyBacktestConfiguration(
    String instrumentId,
    CandleInterval candleInterval,
    BarSeries barSeries,
    Strategy strategy,
    LocalDate from,
    LocalDate to,
    BiConsumer<BarSeries, TradingRecord> strategyAnalysis
  ) {
    this.instrumentId = instrumentId;
    this.candleInterval = candleInterval;
    this.barSeries = barSeries;
    this.strategy = strategy;
    this.from = from;
    this.to = to;
    this.strategyAnalysis = strategyAnalysis;
  }

  public String getInstrumentId() {
    return instrumentId;
  }

  public CandleInterval getCandleInterval() {
    return candleInterval;
  }

  public BarSeries getBarSeries() {
    return barSeries;
  }

  public Strategy getStrategy() {
    return strategy;
  }

  public LocalDate getFrom() {
    return from;
  }

  public LocalDate getTo() {
    return to;
  }

  public static Builder builder() {
    return new Builder();
  }

  public BiConsumer<BarSeries, TradingRecord> getStrategyAnalysis() {
    return strategyAnalysis;
  }

  public static class Builder {

    private String instrumentId;
    private CandleInterval candleInterval;
    private BarSeries barSeries;
    private Strategy strategy;
    private LocalDate from;
    private LocalDate to;
    private BiConsumer<BarSeries, TradingRecord> strategyAnalysis;

    public Builder setInstrumentId(String instrumentId) {
      this.instrumentId = instrumentId;
      return this;
    }

    public Builder setCandleInterval(CandleInterval candleInterval) {
      this.candleInterval = candleInterval;
      return this;
    }

    public Builder setStrategy(Function<BarSeries, Strategy> strategyConstructor) {
      this.barSeries = new BaseBarSeriesBuilder().withNumTypeOf(DecimalNum.class).build();
      this.strategy = strategyConstructor.apply(barSeries);
      return this;
    }

    public Builder setFrom(LocalDate from) {
      this.from = from;
      return this;
    }

    public Builder setTo(LocalDate to) {
      this.to = to;
      return this;
    }

    public Builder setStrategyAnalysis(BiConsumer<BarSeries, TradingRecord> strategyAnalysis) {
      this.strategyAnalysis = strategyAnalysis;
      return this;
    }

    public CandleStrategyBacktestConfiguration build() {
      if (from == null || to == null) {
        throw new IllegalArgumentException("'from' and 'to' should be specified!");
      }
      if (from.isAfter(to)) {
        throw new IllegalArgumentException("'from' should be before 'to'!");
      }
      return new CandleStrategyBacktestConfiguration(
        instrumentId, candleInterval, barSeries, strategy, from, to, strategyAnalysis
      );
    }
  }
}
