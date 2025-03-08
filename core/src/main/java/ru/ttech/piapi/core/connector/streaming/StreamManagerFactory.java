package ru.ttech.piapi.core.connector.streaming;

import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
import ru.ttech.piapi.core.impl.marketdata.MarketDataStreamManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Фабрика менеджеров стримов
 */
public class StreamManagerFactory {

  private final StreamServiceStubFactory streamFactory;

  private StreamManagerFactory(StreamServiceStubFactory streamFactory) {
    this.streamFactory = streamFactory;
  }

  /**
   * Метод для создания фабрики менеджера стримов
   *
   * @param streamFactory Фабрика стримов
   * @return Фабрика менеджеров стримов
   */
  public static StreamManagerFactory create(StreamServiceStubFactory streamFactory) {
    return new StreamManagerFactory(streamFactory);
  }

  /**
   * Метод для создания менеджера стримов рыночных данных
   *
   * @param executorService Пул потоков для выполнения задач {@link OnNextListener}
   * @param scheduledExecutorService Пул потоков для контроля состояния стримов {@link OnNextListener}
   * @return Менеджер стримов рыночных данных
   */
  public MarketDataStreamManager newMarketDataStreamManager(
    ExecutorService executorService,
    ScheduledExecutorService scheduledExecutorService
  ) {
    return new MarketDataStreamManager(streamFactory, executorService, scheduledExecutorService);
  }
}
