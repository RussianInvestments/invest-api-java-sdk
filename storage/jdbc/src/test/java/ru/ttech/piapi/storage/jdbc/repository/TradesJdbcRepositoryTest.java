package ru.ttech.piapi.storage.jdbc.repository;

import com.google.protobuf.Timestamp;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import ru.tinkoff.piapi.contract.v1.Trade;
import ru.tinkoff.piapi.contract.v1.TradeDirection;
import ru.tinkoff.piapi.contract.v1.TradeSourceType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.storage.jdbc.config.JdbcConfiguration;

import java.math.BigDecimal;
import java.time.Instant;

public class TradesJdbcRepositoryTest extends BaseJdbcRepositoryTest<Trade, TradesJdbcRepository> {

  @BeforeEach
  void setUpRepository() {
    postgresRepository = createRepository(createJdbcConfiguration(pgDataSource, "trades"));
    mySqlRepository = createRepository(createJdbcConfiguration(mysqlDataSource, "trades"));
  }

  @SneakyThrows
  @Override
  protected TradesJdbcRepository createRepository(JdbcConfiguration configuration) {
    return new TradesJdbcRepository(configuration);
  }

  @Override
  protected Trade createEntity(String instrumentUid, Instant instant) {
    return Trade.newBuilder()
      .setTime(getTimestampFromInstant(instant))
      .setInstrumentUid(instrumentUid)
      .setDirection(TradeDirection.TRADE_DIRECTION_BUY)
      .setPrice(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(100.0)))
      .setQuantity(100)
      .setTradeSource(TradeSourceType.TRADE_SOURCE_ALL)
      .build();
  }

  @Override
  protected Timestamp getEntityTime(Trade entity) {
    return entity.getTime();
  }

  @Override
  protected String getEntityInstrumentUid(Trade entity) {
    return entity.getInstrumentUid();
  }
}
