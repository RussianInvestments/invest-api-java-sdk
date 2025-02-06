package ru.ttech.piapi.strategy;

import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.strategy.candle.backtest.CandleStrategyBacktest;
import ru.ttech.piapi.strategy.candle.backtest.CandleStrategyBacktestConfiguration;
import ru.ttech.piapi.strategy.candle.backtest.HistoryDataApiClient;
import ru.ttech.piapi.strategy.candle.live.CandleStrategy;
import ru.ttech.piapi.strategy.candle.live.CandleStrategyConfiguration;

public class StrategyFactory {

  private final StreamServiceStubFactory streamFactory;

  public StrategyFactory(StreamServiceStubFactory streamFactory) {
    this.streamFactory = streamFactory;
  }

  public CandleStrategy newCandleStrategy(CandleStrategyConfiguration configuration) {
    return new CandleStrategy(configuration, streamFactory);
  }

  public CandleStrategyBacktest newCandleStrategyBacktest(CandleStrategyBacktestConfiguration configuration) {
    var connectorConfiguration = streamFactory.getServiceStubFactory().getConfiguration();
    var httpApiClient = new HistoryDataApiClient(connectorConfiguration);
    return new CandleStrategyBacktest(configuration, httpApiClient);
  }

  public static StrategyFactory create(StreamServiceStubFactory streamFactory) {
    return new StrategyFactory(streamFactory);
  }
}
