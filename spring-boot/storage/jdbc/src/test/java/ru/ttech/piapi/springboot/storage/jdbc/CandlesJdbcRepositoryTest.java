package ru.ttech.piapi.springboot.storage.jdbc;

import com.google.protobuf.Timestamp;
import lombok.SneakyThrows;
import ru.tinkoff.piapi.contract.v1.Candle;
import ru.tinkoff.piapi.contract.v1.CandleSource;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.springboot.storage.jdbc.config.JdbcConfiguration;
import ru.ttech.piapi.springboot.storage.jdbc.repository.CandlesJdbcRepository;

import java.math.BigDecimal;
import java.util.UUID;

public class CandlesJdbcRepositoryTest extends BaseJdbcRepositoryTest<Candle, CandlesJdbcRepository> {

  @SneakyThrows
  @Override
  protected CandlesJdbcRepository createRepository(JdbcConfiguration configuration) {
    return new CandlesJdbcRepository(configuration);
  }

  @Override
  protected Candle createEntity() {
    return Candle.newBuilder()
      .setTime(getTimestampNow())
      .setInstrumentUid(UUID.randomUUID().toString())
      .setInterval(SubscriptionInterval.SUBSCRIPTION_INTERVAL_2_MIN)
      .setOpen(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(10.0)))
      .setHigh(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(10.0)))
      .setLow(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(10.0)))
      .setClose(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(10.0)))
      .setVolume(1000000L)
      .setLastTradeTs(getTimestampNow())
      .setCandleSourceType(CandleSource.CANDLE_SOURCE_EXCHANGE)
      .build();
  }

  @Override
  protected Timestamp getEntityTime(Candle entity) {
    return entity.getTime();
  }

  @Override
  protected String getEntityInstrumentUid(Candle entity) {
    return entity.getInstrumentUid();
  }
}
