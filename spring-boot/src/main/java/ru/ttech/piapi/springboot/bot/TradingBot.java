package ru.ttech.piapi.springboot.bot;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;

public interface TradingBot {

  CandleInstrument getInstrument();

  GetCandlesRequest.CandleSource getCandleSource();

  int getWarmupLength();

  Strategy getStrategy(BarSeries barSeries);

  void onStrategyEnterAction(Bar bar);

  void onStrategyExitAction(Bar bar);
}
