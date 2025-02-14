package ru.ttech.piapi.core.connector.streaming;

import ru.ttech.piapi.core.impl.marketdata.MarketDataStreamManager;

public class StreamManagerFactory {

  private final StreamServiceStubFactory streamFactory;

  private StreamManagerFactory(StreamServiceStubFactory streamFactory) {
    this.streamFactory = streamFactory;
  }

  public static StreamManagerFactory create(StreamServiceStubFactory streamFactory) {
    return new StreamManagerFactory(streamFactory);
  }

  public MarketDataStreamManager newMarketDataStreamManager() {
    return new MarketDataStreamManager(streamFactory);
  }
}
