package ru.ttech.piapi.storage.jdbc.repository;

import com.google.protobuf.Timestamp;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import ru.tinkoff.piapi.contract.v1.Candle;
import ru.tinkoff.piapi.contract.v1.CandleSource;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.storage.jdbc.config.JdbcConfiguration;

import java.math.BigDecimal;
import java.time.Instant;

public class CandlesJdbcRepositoryTest extends BaseJdbcRepositoryTest<Candle, CandlesJdbcRepository> {

  @BeforeEach
  void setUpRepository() {
    postgresRepository = createRepository(createJdbcConfiguration(pgDataSource, "candles"));
    mySqlRepository = createRepository(createJdbcConfiguration(mysqlDataSource, "candles"));
  }

  @SneakyThrows
  @Override
  protected CandlesJdbcRepository createRepository(JdbcConfiguration configuration) {
    return new CandlesJdbcRepository(configuration);
  }

  @Override
  protected Candle createEntity(String instrumentUid, Instant instant) {
    return Candle.newBuilder()
      .setTime(getTimestampFromInstant(instant))
      .setInstrumentUid(instrumentUid)
      .setInterval(SubscriptionInterval.SUBSCRIPTION_INTERVAL_2_MIN)
      .setOpen(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(10.0)))
      .setHigh(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(10.0)))
      .setLow(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(10.0)))
      .setClose(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(10.0)))
      .setVolume(1000000L)
      .setLastTradeTs(getTimestampFromInstant(instant))
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
