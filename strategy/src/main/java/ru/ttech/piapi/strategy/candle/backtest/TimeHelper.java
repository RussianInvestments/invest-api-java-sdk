package ru.ttech.piapi.strategy.candle.backtest;

import ru.tinkoff.piapi.contract.v1.CandleInterval;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public class TimeHelper {

  public static ZonedDateTime roundFloorStartTime(ZonedDateTime startTime, CandleInterval candleInterval) {
    switch (candleInterval) {
      case CANDLE_INTERVAL_2_MIN:
        return startTime.withMinute(startTime.getMinute() - startTime.getMinute() % 2);
      case CANDLE_INTERVAL_3_MIN:
        return startTime.withMinute(startTime.getMinute() - startTime.getMinute() % 3);
      case CANDLE_INTERVAL_5_MIN:
        return startTime.withMinute(startTime.getMinute() - startTime.getMinute() % 5);
      case CANDLE_INTERVAL_10_MIN:
        return startTime.withMinute(startTime.getMinute() - startTime.getMinute() % 10);
      case CANDLE_INTERVAL_15_MIN:
        return startTime.withMinute(startTime.getMinute() - startTime.getMinute() % 15);
      case CANDLE_INTERVAL_30_MIN:
        return startTime.withMinute(startTime.getMinute() - startTime.getMinute() % 30);
      case CANDLE_INTERVAL_HOUR:
        return startTime.truncatedTo(ChronoUnit.HOURS);
      case CANDLE_INTERVAL_2_HOUR:
        return startTime.truncatedTo(ChronoUnit.HOURS).withHour(startTime.getHour() - startTime.getHour() % 2);
      case CANDLE_INTERVAL_4_HOUR:
        return startTime.truncatedTo(ChronoUnit.HOURS).withHour(startTime.getHour() - startTime.getHour() % 4);
      case CANDLE_INTERVAL_DAY:
        return startTime.withHour(0).withMinute(0);
      case CANDLE_INTERVAL_WEEK:
        return startTime.withHour(0).withMinute(0).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      case CANDLE_INTERVAL_MONTH:
        return startTime.withHour(0).withMinute(0).withDayOfMonth(1);
      default:
        return startTime;
    }
  }

  public static ZonedDateTime getEndTime(ZonedDateTime startTime, CandleInterval candleInterval) {
    switch (candleInterval) {
      case CANDLE_INTERVAL_1_MIN:
        return startTime.plus(1, ChronoUnit.MINUTES);
      case CANDLE_INTERVAL_2_MIN:
        return startTime.plus(2, ChronoUnit.MINUTES);
      case CANDLE_INTERVAL_3_MIN:
        return startTime.plus(3, ChronoUnit.MINUTES);
      case CANDLE_INTERVAL_5_MIN:
        return startTime.plus(5, ChronoUnit.MINUTES);
      case CANDLE_INTERVAL_10_MIN:
        return startTime.plus(10, ChronoUnit.MINUTES);
      case CANDLE_INTERVAL_15_MIN:
        return startTime.plus(15, ChronoUnit.MINUTES);
      case CANDLE_INTERVAL_30_MIN:
        return startTime.plus(30, ChronoUnit.MINUTES);
      case CANDLE_INTERVAL_HOUR:
        return startTime.truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS);
      case CANDLE_INTERVAL_2_HOUR:
        return startTime.truncatedTo(ChronoUnit.HOURS).plus(2, ChronoUnit.HOURS);
      case CANDLE_INTERVAL_4_HOUR:
        return startTime.truncatedTo(ChronoUnit.HOURS).plus(4, ChronoUnit.HOURS);
      case CANDLE_INTERVAL_DAY:
        return startTime.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS);
      case CANDLE_INTERVAL_WEEK:
        return startTime.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.WEEKS);
      case CANDLE_INTERVAL_MONTH:
        return startTime.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.MONTHS);
      default:
        return startTime;
    }
  }
}
