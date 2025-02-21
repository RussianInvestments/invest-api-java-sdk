package ru.ttech.piapi.strategy.candle.live;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.Strategy;
import org.ta4j.core.num.DecimalNum;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Конфигурация стратегии на основе японских свечей
 */
public class CandleStrategyConfiguration {

  private final GetCandlesRequest.CandleSource candleSource;
  private final int warmupLength;
  private final Map<CandleInstrument, Strategy> strategiesMap;
  private final Map<CandleInstrument, BarSeries> barSeriesMap;
  private final BiConsumer<CandleInstrument, Bar> enterAction;
  private final BiConsumer<CandleInstrument, Bar> exitAction;

  private CandleStrategyConfiguration(
    GetCandlesRequest.CandleSource candleSource,
    int warmupLength,
    Map<CandleInstrument, Strategy> strategiesMap,
    Map<CandleInstrument, BarSeries> barSeriesMap,
    BiConsumer<CandleInstrument, Bar> enterAction,
    BiConsumer<CandleInstrument, Bar> exitAction
  ) {
    this.candleSource = candleSource;
    this.warmupLength = warmupLength;
    this.strategiesMap = strategiesMap;
    this.barSeriesMap = barSeriesMap;
    this.enterAction = enterAction;
    this.exitAction = exitAction;
  }

  public GetCandlesRequest.CandleSource getCandleSource() {
    return candleSource;
  }

  public int getWarmupLength() {
    return warmupLength;
  }

  public Map<CandleInstrument, Strategy> getStrategiesMap() {
    return strategiesMap;
  }

  public Map<CandleInstrument, BarSeries> getBarSeriesMap() {
    return barSeriesMap;
  }

  public BiConsumer<CandleInstrument, Bar> getEnterAction() {
    return enterAction;
  }

  public BiConsumer<CandleInstrument, Bar> getExitAction() {
    return exitAction;
  }

  /**
   * Билдер конфигурации стратегии
   * <p>Пример использования:
   * <pre>{@code
   *     var ttechShare = CandleInstrument.newBuilder()
   *       .setInstrumentId("87db07bc-0e02-4e29-90bb-05e8ef791d7b")
   *       .setInterval(SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE)
   *       .build();
   *     var sberShare = CandleInstrument.newBuilder()
   *       .setInstrumentId("e6123145-9665-43e0-8413-cd61b8aa9b13")
   *       .setInterval(SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE)
   *       .build();
   *
   *     var strategyConfiguration = CandleStrategyConfiguration.builder()
   *       .setCandleSource(GetCandlesRequest.CandleSource.CANDLE_SOURCE_INCLUDE_WEEKEND)
   *       .setWarmupLength(100)
   *       .setStrategies(Map.of(
   *         ttechShare, createStrategy(5, 15),
   *         sberShare, createStrategy(10, 20)
   *       ))
   *       .setStrategyEnterAction((candleInstrument, bar) ->
   *         logger.info("Entering strategy for instrument {} with interval {}",
   *           candleInstrument.getInstrumentId(),
   *           candleInstrument.getInterval()
   *         ))
   *       .setStrategyExitAction((candleInstrument, bar) ->
   *         logger.info("Exiting  strategy for instrument {} with interval {}",
   *           candleInstrument.getInstrumentId(),
   *           candleInstrument.getInterval()
   *         ))
   *       .build()
   * }</pre>
   *
   * @return Билдер конфигурации стратегии
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private GetCandlesRequest.CandleSource candleSource;
    private int warmupLength;
    private Map<CandleInstrument, Function<BarSeries, Strategy>> strategyConstructors;
    private BiConsumer<CandleInstrument, Bar> enterAction;
    private BiConsumer<CandleInstrument, Bar> exitAction;

    /**
     * Установка источника свечей
     *
     * @param candleSource Источник свечей
     * @return Билдер конфигурации стратегии
     */
    public Builder setCandleSource(GetCandlesRequest.CandleSource candleSource) {
      this.candleSource = candleSource;
      return this;
    }

    /**
     * Установка количества свечей, которые будут загружены в серию для корректного вычисления значений индикаторов
     *
     * @param warmupLength Длина периода в свечах
     * @return Билдер конфигурации стратегии
     */
    public Builder setWarmupLength(int warmupLength) {
      this.warmupLength = warmupLength;
      return this;
    }

    /**
     * Установка инструментов ({@link CandleInstrument}) и стратегий ({@link Strategy}) к ним
     *
     * @param strategyConstructors {@link Map} инструментов и стратегий. Стратегия должна быть задана в виде:
     *                                <pre>{@code
     *                                Map.of(ttechShare, barSeries -> {
     *                                       ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
     *                                       SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
     *                                       SMAIndicator longSma = new SMAIndicator(closePrice, 30);
     *                                       Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma);
     *                                       Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma);
     *                                       return new BaseStrategy(buyingRule, sellingRule);
     *                                      })
     *                                }</pre>
     * @return Билдер конфигурации стратегии
     */
    public Builder setStrategies(Map<CandleInstrument, Function<BarSeries, Strategy>> strategyConstructors) {
      this.strategyConstructors = strategyConstructors;
      return this;
    }

    /**
     * Установка действия при сигнале на вход по стратегии на конкретном инструменте
     *
     * @param entryAction {@link BiConsumer} в котором доступны инструмент ({@link CandleInstrument}) и свеча ({@link Bar})
     *                     <pre>{@code
     *                          (candleInstrument, bar) ->
     *                                 logger.info("Entering by strategy for instrument {} with interval {}",
     *                                             candleInstrument.getInstrumentId(),
     *                                             candleInstrument.getInterval())
     *                    }</pre>
     * @return Билдер конфигурации стратегии
     */
    public Builder setStrategyEnterAction(BiConsumer<CandleInstrument, Bar> entryAction) {
      this.enterAction = entryAction;
      return this;
    }

    /**
     * Установка действия при сигнале на выход по стратегии на конкретном инструменте
     *
     * @param exitAction {@link BiConsumer} в котором доступны инструмент ({@link CandleInstrument}) и свеча ({@link Bar})
     *                   <pre>{@code
     *                   (candleInstrument, bar) ->
     *                          logger.info("Exiting by strategy for instrument {} with interval {}",
     *                                      candleInstrument.getInstrumentId(),
     *                                      candleInstrument.getInterval())
     *                   }</pre>
     * @return Билдер конфигурации стратегии
     */
    public Builder setStrategyExitAction(BiConsumer<CandleInstrument, Bar> exitAction) {
      this.exitAction = exitAction;
      return this;
    }

    /**
     * Создание конфигурации стратегии
     *
     * @return Конфигурация стратегии на основе японских свечей
     */
    public CandleStrategyConfiguration build() {
      if (strategyConstructors == null || strategyConstructors.isEmpty()) {
        throw new IllegalStateException("Strategy constructors is not set");
      }
      var barSeriesMap = strategyConstructors.keySet().stream()
        .collect(Collectors.toMap(
          instrument -> instrument,
          instrument -> (BarSeries) new BaseBarSeriesBuilder()
            .withName(instrument.getInstrumentId())
            .withMaxBarCount(warmupLength)
            .withNumTypeOf(DecimalNum.class)
            .build()
          ));
      var strategiesMap = strategyConstructors.entrySet().stream()
        .map(entry -> Tuple.of(entry.getKey(), entry.getValue().apply(barSeriesMap.get(entry.getKey()))))
        .collect(Collectors.toMap(Tuple2::_1, Tuple2::_2));
      return new CandleStrategyConfiguration(
        candleSource, warmupLength, strategiesMap, barSeriesMap, enterAction, exitAction
      );
    }
  }
}
