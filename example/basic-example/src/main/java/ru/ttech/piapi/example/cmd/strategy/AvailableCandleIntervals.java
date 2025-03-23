package ru.ttech.piapi.example.cmd.strategy;

import ru.tinkoff.piapi.contract.v1.CandleInterval;

import java.util.ArrayList;
import java.util.List;

public class AvailableCandleIntervals extends ArrayList<String> {

  public AvailableCandleIntervals() {
    super(List.of(
      CandleInterval.CANDLE_INTERVAL_1_MIN.name(),
      CandleInterval.CANDLE_INTERVAL_2_MIN.name(),
      CandleInterval.CANDLE_INTERVAL_3_MIN.name(),
      CandleInterval.CANDLE_INTERVAL_5_MIN.name(),
      CandleInterval.CANDLE_INTERVAL_10_MIN.name(),
      CandleInterval.CANDLE_INTERVAL_15_MIN.name(),
      CandleInterval.CANDLE_INTERVAL_30_MIN.name(),
      CandleInterval.CANDLE_INTERVAL_HOUR.name(),
      CandleInterval.CANDLE_INTERVAL_2_HOUR.name(),
      CandleInterval.CANDLE_INTERVAL_4_HOUR.name(),
      CandleInterval.CANDLE_INTERVAL_DAY.name(),
      CandleInterval.CANDLE_INTERVAL_WEEK.name(),
      CandleInterval.CANDLE_INTERVAL_MONTH.name()
    ));
  }
}
