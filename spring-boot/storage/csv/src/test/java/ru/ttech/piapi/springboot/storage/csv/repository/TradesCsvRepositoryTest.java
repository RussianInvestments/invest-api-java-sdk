package ru.ttech.piapi.springboot.storage.csv.repository;

import com.google.protobuf.Timestamp;
import lombok.SneakyThrows;
import ru.tinkoff.piapi.contract.v1.Trade;
import ru.tinkoff.piapi.contract.v1.TradeDirection;
import ru.tinkoff.piapi.contract.v1.TradeSourceType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.csv.config.CsvConfiguration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TradesCsvRepositoryTest extends BaseCsvRepositoryTest<Trade> {

  @SneakyThrows
  @Override
  protected CsvRepository<Trade> createRepository(CsvConfiguration config) {
    return new TradesCsvRepository(config);
  }

  @Override
  protected Trade createEntity() {
    return Trade.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(LocalDateTime.now()))
      .setInstrumentUid(UUID.randomUUID().toString())
      .setDirection(TradeDirection.TRADE_DIRECTION_BUY)
      .setPrice(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(100.16)))
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
