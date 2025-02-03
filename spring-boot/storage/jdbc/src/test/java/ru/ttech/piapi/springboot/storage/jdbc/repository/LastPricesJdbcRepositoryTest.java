package ru.ttech.piapi.springboot.storage.jdbc.repository;

import com.google.protobuf.Timestamp;
import lombok.SneakyThrows;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.LastPriceType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.springboot.storage.jdbc.config.JdbcConfiguration;

import java.math.BigDecimal;
import java.time.Instant;

public class LastPricesJdbcRepositoryTest extends BaseJdbcRepositoryTest<LastPrice, LastPricesJdbcRepository> {

  @SneakyThrows
  @Override
  protected LastPricesJdbcRepository createRepository(JdbcConfiguration configuration) {
    return new LastPricesJdbcRepository(configuration);
  }

  @Override
  protected LastPrice createEntity(String instrumentUid, Instant instant) {
    return LastPrice.newBuilder()
      .setTime(getTimestampFromInstant(instant))
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
