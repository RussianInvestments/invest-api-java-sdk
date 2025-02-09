package ru.ttech.piapi.strategy.candle.backtest;

import org.ta4j.core.analysis.cost.CostModel;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.backtest.TradeExecutionModel;
import ru.tinkoff.piapi.contract.v1.CandleInterval;

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class CandleStrategyBacktestConfiguration {

  private final String instrumentId;
  private final CandleInterval candleInterval;
  private final CostModel tradeFeeModel;
  private final TradeExecutionModel tradeExecutionModel;
  private final ExecutorService executorService;
  private final LocalDate from;
  private final LocalDate to;
  private final Consumer<BarSeriesManager> strategyAnalysis;

  private CandleStrategyBacktestConfiguration(
    String instrumentId,
    CandleInterval candleInterval,
    CostModel tradeFeeModel,
    TradeExecutionModel tradeExecutionModel,
    ExecutorService executorService,
    LocalDate from,
    LocalDate to,
    Consumer<BarSeriesManager> strategyAnalysis
  ) {
    this.instrumentId = instrumentId;
    this.candleInterval = candleInterval;
    this.tradeFeeModel = tradeFeeModel;
    this.tradeExecutionModel = tradeExecutionModel;
    this.executorService = executorService;
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

  public LocalDate getFrom() {
    return from;
  }

  public LocalDate getTo() {
    return to;
  }

  public CostModel getTradeFeeModel() {
    return tradeFeeModel;
  }

  public TradeExecutionModel getTradeExecutionModel() {
    return tradeExecutionModel;
  }

  public ExecutorService getExecutorService() {
    return executorService;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Consumer<BarSeriesManager> getStrategyAnalysis() {
    return strategyAnalysis;
  }

  public static class Builder {

    private String instrumentId;
    private CandleInterval candleInterval;
    private CostModel tradeFeeModel;
    private TradeExecutionModel tradeExecutionModel;
    private ExecutorService executorService;
    private LocalDate from;
    private LocalDate to;
    private Consumer<BarSeriesManager> strategyAnalysis;

    public Builder setInstrumentId(String instrumentId) {
      this.instrumentId = instrumentId;
      return this;
    }

    public Builder setCandleInterval(CandleInterval candleInterval) {
      this.candleInterval = candleInterval;
      return this;
    }

    public Builder setTradeFeeModel(CostModel costModel) {
      this.tradeFeeModel = costModel;
      return this;
    }

    public Builder setTradeExecutionModel(TradeExecutionModel tradeExecutionModel) {
      this.tradeExecutionModel = tradeExecutionModel;
      return this;
    }

    public Builder setExecutorService(ExecutorService executorService) {
      this.executorService = executorService;
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

    public Builder setStrategyAnalysis(Consumer<BarSeriesManager> strategyAnalysis) {
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
        instrumentId, candleInterval, tradeFeeModel, tradeExecutionModel, executorService, from, to, strategyAnalysis
      );
    }
  }
}
