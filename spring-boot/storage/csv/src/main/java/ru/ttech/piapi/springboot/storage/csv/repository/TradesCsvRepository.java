package ru.ttech.piapi.springboot.storage.csv.repository;

import org.apache.commons.csv.CSVRecord;
import ru.tinkoff.piapi.contract.v1.Trade;
import ru.tinkoff.piapi.contract.v1.TradeDirection;
import ru.tinkoff.piapi.contract.v1.TradeSourceType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.csv.config.CsvConfiguration;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TradesCsvRepository extends CsvRepository<Trade> {

  public TradesCsvRepository(CsvConfiguration configuration) throws IOException {
    super(configuration);
  }

  @Override
  protected String[] getHeaders() {
    return new String[]{"time", "instrument_uid", "direction", "price", "quantity", "trade_source"};
  }

  @Override
  protected Iterable<?> convertToIterable(Trade entity) {
    return List.of(
      TimeMapper.timestampToLocalDateTime(entity.getTime()),
      entity.getInstrumentUid(),
      entity.getDirection(),
      entity.getPrice(),
      entity.getQuantity(),
      entity.getTradeSource()
    );
  }

  @Override
  protected Trade convertToEntity(CSVRecord csvRecord) {
    return Trade.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(LocalDateTime.parse(csvRecord.get("time"))))
      .setInstrumentUid(csvRecord.get("instrument_uid"))
      .setDirection(TradeDirection.valueOf(csvRecord.get("direction")))
      .setPrice(NumberMapper.bigDecimalToQuotation(new BigDecimal(csvRecord.get("price"))))
      .setQuantity(Long.parseLong(csvRecord.get("quantity")))
      .setTradeSource(TradeSourceType.valueOf(csvRecord.get("trade_source")))
      .build();
  }
}
