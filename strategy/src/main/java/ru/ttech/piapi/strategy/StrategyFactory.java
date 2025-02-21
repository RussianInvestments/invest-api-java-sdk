package ru.ttech.piapi.strategy;

import ru.ttech.piapi.core.impl.marketdata.MarketDataStreamManager;
import ru.ttech.piapi.strategy.candle.live.CandleStrategy;
import ru.ttech.piapi.strategy.candle.live.CandleStrategyConfiguration;

/**
 * Фабрика торговых стратегий
 * <p>На данный момент поддерживаются следующие стратегии:
 * <ul>
 *   <li>Стратегия на основе свечей {@link CandleStrategy}</li>
 * </ul>
 */
public class StrategyFactory {

  private final MarketDataStreamManager marketDataStreamManager;

  private StrategyFactory(MarketDataStreamManager marketDataStreamManager) {
    this.marketDataStreamManager = marketDataStreamManager;
  }

  /**
   * Метод для создания новой стратегии основанной на японских свечах
   *
   * @param configuration Конфигурация стратегии японских свечей
   * @return Стратегия на основе японских свечей
   */
  public CandleStrategy newCandleStrategy(
    CandleStrategyConfiguration configuration
  ) {
    return new CandleStrategy(configuration, marketDataStreamManager);
  }

  /**
   * Метод для создания фабрики стратегий
   *
   * @param marketDataStreamManager Менеджер стримов рыночных данных
   * @return Фабрика стратегий
   */
  public static StrategyFactory create(MarketDataStreamManager marketDataStreamManager) {
    return new StrategyFactory(marketDataStreamManager);
  }
}
