package ru.ttech.piapi.storage.csv.repository;

import com.google.protobuf.Timestamp;
import lombok.SneakyThrows;
import ru.tinkoff.piapi.contract.v1.Trade;
import ru.tinkoff.piapi.contract.v1.TradeDirection;
import ru.tinkoff.piapi.contract.v1.TradeSourceType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.storage.csv.config.CsvConfiguration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradesCsvRepositoryTest extends BaseCsvRepositoryTest<Trade> {

  @SneakyThrows
  @Override
  protected CsvRepository<Trade> createRepository(CsvConfiguration config) {
    return new TradesCsvRepository(config);
  }

  @Override
  protected Trade createEntity(LocalDateTime time, String instrumentUid) {
    return Trade.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(time))
      .setInstrumentUid(instrumentUid)
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
