package ru.ttech.piapi.storage.jdbc.repository;

import com.google.protobuf.Timestamp;
import lombok.SneakyThrows;
import ru.tinkoff.piapi.contract.v1.SecurityTradingStatus;
import ru.tinkoff.piapi.contract.v1.TradingStatus;
import ru.ttech.piapi.storage.jdbc.config.JdbcConfiguration;

import java.time.Instant;

public class TradingStatusesJdbcRepositoryTest extends BaseJdbcRepositoryTest<TradingStatus, TradingStatusesJdbcRepository> {

  @SneakyThrows
  @Override
  protected TradingStatusesJdbcRepository createRepository(JdbcConfiguration configuration) {
    return new TradingStatusesJdbcRepository(configuration);
  }

  @Override
  protected TradingStatus createEntity(String instrumentUid, Instant instant) {
    return TradingStatus.newBuilder()
      .setTime(getTimestampFromInstant(instant))
      .setInstrumentUid(instrumentUid)
      .setTradingStatus(SecurityTradingStatus.SECURITY_TRADING_STATUS_NORMAL_TRADING)
      .setLimitOrderAvailableFlag(true)
      .setMarketOrderAvailableFlag(true)
      .build();
  }

  @Override
  protected Timestamp getEntityTime(TradingStatus entity) {
    return entity.getTime();
  }

  @Override
  protected String getEntityInstrumentUid(TradingStatus entity) {
    return entity.getInstrumentUid();
  }
}
