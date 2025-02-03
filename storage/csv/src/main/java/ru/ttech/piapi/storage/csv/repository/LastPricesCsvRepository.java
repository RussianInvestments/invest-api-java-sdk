package ru.ttech.piapi.storage.csv.repository;

import org.apache.commons.csv.CSVRecord;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.LastPriceType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.storage.csv.config.CsvConfiguration;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class LastPricesCsvRepository extends CsvRepository<LastPrice> {

  public LastPricesCsvRepository(CsvConfiguration configuration) throws IOException {
    super(configuration);
  }

  @Override
  protected String[] getHeaders() {
    return new String[]{"time", "instrument_uid", "price", "last_price_type"};
  }

  @Override
  protected Iterable<?> convertToIterable(LastPrice entity) {
    return List.of(
      TimeMapper.timestampToLocalDateTime(entity.getTime()),
      entity.getInstrumentUid(),
      NumberMapper.quotationToBigDecimal(entity.getPrice()),
      entity.getLastPriceType()
    );
  }

  @Override
  protected LastPrice convertToEntity(CSVRecord csvRecord) {
    return LastPrice.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(LocalDateTime.parse(csvRecord.get("time"))))
      .setInstrumentUid(csvRecord.get("instrument_uid"))
      .setPrice(NumberMapper.bigDecimalToQuotation(new BigDecimal(csvRecord.get("price"))))
      .setLastPriceType(LastPriceType.valueOf(csvRecord.get("last_price_type")))
      .build();
  }
}
