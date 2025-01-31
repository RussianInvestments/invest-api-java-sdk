package ru.ttech.piapi.springboot.storage.csv.repository;

import ru.tinkoff.piapi.contract.v1.Candle;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.csv.config.CsvConfiguration;

import java.io.IOException;
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
  public Iterable<Object> convertToIterable(Candle candle) {
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
}
