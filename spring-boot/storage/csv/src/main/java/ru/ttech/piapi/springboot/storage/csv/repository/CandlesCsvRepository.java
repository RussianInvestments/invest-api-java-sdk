package ru.ttech.piapi.springboot.storage.csv.repository;

import org.apache.commons.csv.CSVRecord;
import ru.tinkoff.piapi.contract.v1.Candle;
import ru.tinkoff.piapi.contract.v1.CandleSource;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.csv.config.CsvConfiguration;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CandlesCsvRepository extends CsvRepository<Candle> {

  public CandlesCsvRepository(CsvConfiguration configuration) throws IOException {
    super(configuration);
  }

  @Override
  protected String[] getHeaders() {
    return new String[]{"time", "instrument_uid", "interval", "open", "high", "low", "close", "volume",
      "last_trade_ts", "candle_source_type"};
  }

  @Override
  protected Iterable<?> convertToIterable(Candle candle) {
    return List.of(
      TimeMapper.timestampToLocalDateTime(candle.getTime()),
      candle.getInstrumentUid(),
      candle.getInterval(),
      NumberMapper.quotationToBigDecimal(candle.getOpen()),
      NumberMapper.quotationToBigDecimal(candle.getHigh()),
      NumberMapper.quotationToBigDecimal(candle.getLow()),
      NumberMapper.quotationToBigDecimal(candle.getClose()),
      candle.getVolume(),
      TimeMapper.timestampToLocalDateTime(candle.getLastTradeTs()),
      candle.getCandleSourceType()
    );
  }

  @Override
  protected Candle convertToEntity(CSVRecord csvRecord) {
    return Candle.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(LocalDateTime.parse(csvRecord.get("time"))))
      .setInstrumentUid(csvRecord.get("instrument_uid"))
      .setInterval(SubscriptionInterval.valueOf(csvRecord.get("interval")))
      .setOpen(NumberMapper.bigDecimalToQuotation(new BigDecimal(csvRecord.get("open"))))
      .setHigh(NumberMapper.bigDecimalToQuotation(new BigDecimal(csvRecord.get("high"))))
      .setLow(NumberMapper.bigDecimalToQuotation(new BigDecimal(csvRecord.get("low"))))
      .setClose(NumberMapper.bigDecimalToQuotation(new BigDecimal(csvRecord.get("close"))))
      .setVolume(Long.parseLong(csvRecord.get("volume")))
      .setLastTradeTs(TimeMapper.localDateTimeToTimestamp(LocalDateTime.parse(csvRecord.get("last_trade_ts"))))
      .setCandleSourceType(CandleSource.valueOf(csvRecord.get("candle_source_type")))
      .build();
  }
}
