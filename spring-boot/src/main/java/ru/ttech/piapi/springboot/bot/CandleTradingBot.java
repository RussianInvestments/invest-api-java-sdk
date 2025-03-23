package ru.ttech.piapi.springboot.bot;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;

import java.util.Map;
import java.util.function.Function;

public interface CandleTradingBot {

  GetCandlesRequest.CandleSource getCandleSource();

  int getWarmupLength();

  Map<CandleInstrument, Function<BarSeries, Strategy>> getStrategies();

  void onStrategyEnterAction(CandleInstrument instrument, Bar bar);

  void onStrategyExitAction(CandleInstrument instrument, Bar bar);
}
