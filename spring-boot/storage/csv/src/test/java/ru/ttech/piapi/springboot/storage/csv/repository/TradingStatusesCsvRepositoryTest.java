package ru.ttech.piapi.springboot.storage.csv.repository;

import com.google.protobuf.Timestamp;
import lombok.SneakyThrows;
import ru.tinkoff.piapi.contract.v1.SecurityTradingStatus;
import ru.tinkoff.piapi.contract.v1.TradingStatus;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.csv.config.CsvConfiguration;

import java.time.LocalDateTime;

public class TradingStatusesCsvRepositoryTest extends BaseCsvRepositoryTest<TradingStatus> {

  @SneakyThrows
  @Override
  protected CsvRepository<TradingStatus> createRepository(CsvConfiguration config) {
    return new TradingStatusesCsvRepository(config);
  }

  @Override
  protected TradingStatus createEntity(String instrumentUid) {
    return TradingStatus.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(LocalDateTime.now()))
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
