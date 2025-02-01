package ru.ttech.piapi.springboot.storage.csv.repository;

import org.apache.commons.csv.CSVRecord;
import ru.tinkoff.piapi.contract.v1.SecurityTradingStatus;
import ru.tinkoff.piapi.contract.v1.TradingStatus;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.csv.config.CsvConfiguration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class TradingStatusCsvRepository extends CsvRepository<TradingStatus> {

  public TradingStatusCsvRepository(CsvConfiguration configuration) throws IOException {
    super(configuration);
  }

  @Override
  protected String[] getHeaders() {
    return new String[]{"time", "instrument_uid", "trading_status",
      "limit_order_available_flag", "market_order_available_flag"};
  }

  @Override
  protected Iterable<?> convertToIterable(TradingStatus entity) {
    return List.of(
      TimeMapper.timestampToLocalDateTime(entity.getTime()),
      entity.getInstrumentUid(),
      entity.getTradingStatus(),
      entity.getLimitOrderAvailableFlag(),
      entity.getMarketOrderAvailableFlag()
    );
  }

  @Override
  protected TradingStatus convertToEntity(CSVRecord csvRecord) {
    return TradingStatus.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(LocalDateTime.parse(csvRecord.get("time"))))
      .setInstrumentUid(csvRecord.get("instrument_uid"))
      .setTradingStatus(SecurityTradingStatus.valueOf(csvRecord.get("trading_status")))
      .setLimitOrderAvailableFlag(Boolean.parseBoolean(csvRecord.get("limit_order_available_flag")))
      .setMarketOrderAvailableFlag(Boolean.parseBoolean(csvRecord.get("market_order_available_flag")))
      .build();
  }
}
