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

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MultipleInstrumentsCandleStrategyConfiguration {

  private final List<CandleInstrument> instruments;
  private final GetCandlesRequest.CandleSource candleSource;
  private final int warmupLength;
  private final Map<CandleInstrument, Strategy> strategiesMap;
  private final Map<CandleInstrument, BarSeries> barSeriesMap;
  private final BiConsumer<CandleInstrument, Bar> enterAction;
  private final BiConsumer<CandleInstrument, Bar> exitAction;

  public MultipleInstrumentsCandleStrategyConfiguration(
    List<CandleInstrument> instruments,
    GetCandlesRequest.CandleSource candleSource,
    int warmupLength,
    Map<CandleInstrument, Strategy> strategiesMap,
    Map<CandleInstrument, BarSeries> barSeriesMap,
    BiConsumer<CandleInstrument, Bar> enterAction,
    BiConsumer<CandleInstrument, Bar> exitAction
  ) {
    this.instruments = instruments;
    this.candleSource = candleSource;
    this.warmupLength = warmupLength;
    this.strategiesMap = strategiesMap;
    this.barSeriesMap = barSeriesMap;
    this.enterAction = enterAction;
    this.exitAction = exitAction;
  }

  public List<CandleInstrument> getInstruments() {
    return instruments;
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

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private List<CandleInstrument> instruments;
    private GetCandlesRequest.CandleSource candleSource;
    private int warmupLength;
    private Map<CandleInstrument, Function<BarSeries, Strategy>> strategyConstructors;
    private BiConsumer<CandleInstrument, Bar> enterAction;
    private BiConsumer<CandleInstrument, Bar> exitAction;

    public Builder setInstruments(List<CandleInstrument> instruments) {
      this.instruments = instruments;
      return this;
    }

    public Builder setCandleSource(GetCandlesRequest.CandleSource candleSource) {
      this.candleSource = candleSource;
      return this;
    }

    public Builder setWarmupLength(int warmupLength) {
      this.warmupLength = warmupLength;
      return this;
    }

    public Builder setStrategies(Map<CandleInstrument, Function<BarSeries, Strategy>> strategyConstructors) {
      this.strategyConstructors = strategyConstructors;
      return this;
    }

    public Builder setStrategyEnterAction(BiConsumer<CandleInstrument, Bar> entryAction) {
      this.enterAction = entryAction;
      return this;
    }

    public Builder setStrategyExitAction(BiConsumer<CandleInstrument, Bar> exitAction) {
      this.exitAction = exitAction;
      return this;
    }

    public MultipleInstrumentsCandleStrategyConfiguration build() {
      if (instruments == null || instruments.isEmpty()) {
        throw new IllegalStateException("Instruments is not set");
      }
      if (strategyConstructors == null || strategyConstructors.isEmpty()) {
        throw new IllegalStateException("Strategy constructors is not set");
      }
      instruments.forEach(instrument -> {

      });
      var barSeriesMap = instruments.stream()
        .collect(Collectors.toMap(
          instrument -> instrument,
          instrument -> (BarSeries) new BaseBarSeriesBuilder()
            .withName(instrument.getInstrumentId())
            .withNumTypeOf(DecimalNum.class)
            .build()
          ));
      var strategiesMap = strategyConstructors.entrySet().stream()
        .map(entry -> Tuple.of(entry.getKey(), entry.getValue().apply(barSeriesMap.get(entry.getKey()))))
        .collect(Collectors.toMap(Tuple2::_1, Tuple2::_2));
      return new MultipleInstrumentsCandleStrategyConfiguration(
        instruments, candleSource, warmupLength, strategiesMap, barSeriesMap, enterAction, exitAction
      );
    }
  }
}
