package ru.ttech.piapi.strategy;

import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.strategy.candle.backtest.BarsLoader;
import ru.ttech.piapi.strategy.candle.backtest.CandleStrategyBacktest;
import ru.ttech.piapi.strategy.candle.backtest.CandleStrategyBacktestConfiguration;

public class BacktestStrategyFactory {

  private final ConnectorConfiguration connectorConfiguration;

  private BacktestStrategyFactory(ConnectorConfiguration connectorConfiguration) {
    this.connectorConfiguration = connectorConfiguration;
  }

  public static BacktestStrategyFactory create(ConnectorConfiguration connectorConfiguration) {
    return new BacktestStrategyFactory(connectorConfiguration);
  }

  public CandleStrategyBacktest newCandleStrategyBacktest(CandleStrategyBacktestConfiguration configuration) {
    var barsLoader = new BarsLoader(connectorConfiguration, configuration.getExecutorService());
    return new CandleStrategyBacktest(configuration, barsLoader);
  }
}
