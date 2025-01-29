package ru.ttech.piapi.springboot.storage.csv.repository;

import org.apache.commons.csv.CSVFormat;
import ru.tinkoff.piapi.contract.v1.Candle;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.csv.config.CsvConfiguration;
import ru.ttech.piapi.springboot.storage.csv.driver.CsvWriter;
import ru.ttech.piapi.springboot.storage.repository.CandlesRepository;

public class CandlesCsvRepository implements CandlesRepository {

  private static final String[] HEADERS = {
    "figi", "interval", "open", "high", "low", "close", "volume", "time",
    "last_trade_ts", "instrument_uid", "candle_source_type"
  };
  private static final String FILE_NAME = "candles.csv";
  private static final CSVFormat CSV_FORMAT = CSVFormat.Builder.create(CSVFormat.RFC4180)
    .setHeader(HEADERS)
    .setSkipHeaderRecord(true)
    .get();

  private final CsvConfiguration configuration;
  private final CsvWriter csvWriter = new CsvWriter();

  public CandlesCsvRepository(CsvConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public Iterable<Candle> saveBatch(Iterable<Candle> entities) {
    // TODO: сохранить батч в CSV
    return entities;
  }

  @Override
  public Candle save(Candle entity) {
    csvWriter.write(FILE_NAME, CSV_FORMAT, convertCandleToRow(entity));
    return entity;
  }

  private Object[] convertCandleToRow(Candle candle) {
    return new Object[] {
      candle.getFigi(),
      candle.getInterval(),
      NumberMapper.quotationToBigDecimal(candle.getOpen()),
      NumberMapper.quotationToBigDecimal(candle.getHigh()),
      NumberMapper.quotationToBigDecimal(candle.getLow()),
      NumberMapper.quotationToBigDecimal(candle.getClose()),
      candle.getVolume(),
      TimeMapper.timestampToLocalDate(candle.getTime()),
      TimeMapper.timestampToLocalDate(candle.getLastTradeTs()),
      candle.getInstrumentUid(),
      candle.getCandleSourceType()
    };
  }
}
