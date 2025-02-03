package ru.ttech.piapi.storage.csv.repository;

import com.google.protobuf.Timestamp;
import lombok.SneakyThrows;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.LastPriceType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.storage.csv.config.CsvConfiguration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LastPricesCsvRepositoryTest extends BaseCsvRepositoryTest<LastPrice> {

  @SneakyThrows
  @Override
  protected CsvRepository<LastPrice> createRepository(CsvConfiguration config) {
    return new LastPricesCsvRepository(config);
  }

  @Override
  protected LastPrice createEntity(String instrumentUid) {
    return LastPrice.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(LocalDateTime.now()))
      .setInstrumentUid(instrumentUid)
      .setPrice(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(100.0)))
      .setLastPriceType(LastPriceType.LAST_PRICE_EXCHANGE)
      .build();
  }

  @Override
  protected Timestamp getEntityTime(LastPrice entity) {
    return entity.getTime();
  }

  @Override
  protected String getEntityInstrumentUid(LastPrice entity) {
    return entity.getInstrumentUid();
  }
}
