package ru.ttech.piapi.strategy.candle.mapper;

import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.num.DecimalNum;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;

import java.time.ZoneOffset;

public class BarMapper {

  public static Bar convertCandleWrapperToBar(CandleWrapper candle) {
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

  public static Bar convertHistoricCandleToBar(HistoricCandle candle, SubscriptionInterval interval) {
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
}
