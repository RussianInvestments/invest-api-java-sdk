package ru.ttech.piapi.strategy;

import ru.ttech.piapi.core.impl.marketdata.MarketDataStreamManager;
import ru.ttech.piapi.strategy.candle.live.CandleStrategy;
import ru.ttech.piapi.strategy.candle.live.CandleStrategyConfiguration;
import ru.ttech.piapi.strategy.candle.live.MultipleInstrumentsCandleStrategy;
import ru.ttech.piapi.strategy.candle.live.MultipleInstrumentsCandleStrategyConfiguration;

public class StrategyFactory {

  private final MarketDataStreamManager marketDataStreamManager;

  private StrategyFactory(MarketDataStreamManager marketDataStreamManager) {
    this.marketDataStreamManager = marketDataStreamManager;
  }

  public CandleStrategy newCandleStrategy(CandleStrategyConfiguration configuration) {
    return new CandleStrategy(configuration, marketDataStreamManager);
  }

  public MultipleInstrumentsCandleStrategy newMultipleInstrumentsCandleStrategy(
    MultipleInstrumentsCandleStrategyConfiguration configuration
  ) {
    return new MultipleInstrumentsCandleStrategy(configuration, marketDataStreamManager);
  }

  public static StrategyFactory create(MarketDataStreamManager marketDataStreamManager) {
    return new StrategyFactory(marketDataStreamManager);
  }
}
