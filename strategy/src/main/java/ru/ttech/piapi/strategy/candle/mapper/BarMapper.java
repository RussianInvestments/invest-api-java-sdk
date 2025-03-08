package ru.ttech.piapi.strategy.candle.mapper;

import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.num.DecimalNum;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;
import ru.ttech.piapi.strategy.candle.backtest.BarData;
import ru.ttech.piapi.strategy.candle.backtest.TimeHelper;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class BarMapper {

  public static Bar mapCandleWrapperToBar(CandleWrapper candle) {
    var startTime = candle.getTime();
    var period = PeriodMapper.getTimePeriod(startTime, candle.getInterval());
    var endTime = startTime.plusMinutes(period.toMinutes()).atZone(ZoneOffset.UTC);
    return BaseBar.builder()
      .timePeriod(period)
      .endTime(endTime)
      .openPrice(DecimalNum.valueOf(candle.getOpen()))
      .closePrice(DecimalNum.valueOf(candle.getClose()))
      .lowPrice(DecimalNum.valueOf(candle.getLow()))
      .highPrice(DecimalNum.valueOf(candle.getHigh()))
      .volume(DecimalNum.valueOf(candle.getVolume()))
      .build();
  }

  public static Bar mapHistoricCandleToBar(HistoricCandle candle, SubscriptionInterval interval) {
    var startTime = TimeMapper.timestampToLocalDateTime(candle.getTime());
    var period = PeriodMapper.getTimePeriod(startTime, interval);
    var endTime = startTime.plusMinutes(period.toMinutes()).atZone(ZoneOffset.UTC);
    return BaseBar.builder()
      .timePeriod(period)
      .endTime(endTime)
      .openPrice(DecimalNum.valueOf(NumberMapper.quotationToBigDecimal(candle.getOpen())))
      .closePrice(DecimalNum.valueOf(NumberMapper.quotationToBigDecimal(candle.getClose())))
      .lowPrice(DecimalNum.valueOf(NumberMapper.quotationToBigDecimal(candle.getLow())))
      .highPrice(DecimalNum.valueOf(NumberMapper.quotationToBigDecimal(candle.getHigh())))
      .volume(DecimalNum.valueOf(candle.getVolume()))
      .build();
  }

  public static Bar mapBarDataWithIntervalToBar(BarData barData, CandleInterval interval) {
    var startTime = TimeHelper.roundFloorStartTime(ZonedDateTime.parse(barData.getStartTime()), interval);
    var endTime = TimeHelper.getEndTime(startTime, interval);
    return BaseBar.builder()
      .endTime(endTime)
      .timePeriod(Duration.between(startTime, endTime))
      .openPrice(DecimalNum.valueOf(barData.getOpen()))
      .highPrice(DecimalNum.valueOf(barData.getHigh()))
      .lowPrice(DecimalNum.valueOf(barData.getLow()))
      .closePrice(DecimalNum.valueOf(barData.getClose()))
      .volume(DecimalNum.valueOf(barData.getVolume()))
      .build();
  }
}
