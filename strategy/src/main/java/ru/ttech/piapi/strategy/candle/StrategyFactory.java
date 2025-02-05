package ru.ttech.piapi.strategy.candle;

import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;

public class StrategyFactory {

  private final StreamServiceStubFactory streamFactory;

  public StrategyFactory(StreamServiceStubFactory streamFactory) {
    this.streamFactory = streamFactory;
  }

  public CandleStrategy newCandleStrategy(CandleStrategyConfiguration configuration) {
    return new CandleStrategy(configuration, streamFactory);
  }

  public static StrategyFactory create(StreamServiceStubFactory streamFactory) {
    return new StrategyFactory(streamFactory);
  }
}
