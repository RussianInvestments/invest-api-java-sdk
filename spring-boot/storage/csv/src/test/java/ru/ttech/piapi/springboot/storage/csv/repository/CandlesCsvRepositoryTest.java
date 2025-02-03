package ru.ttech.piapi.springboot.storage.csv.repository;

import com.google.protobuf.Timestamp;
import lombok.SneakyThrows;
import ru.tinkoff.piapi.contract.v1.Candle;
import ru.tinkoff.piapi.contract.v1.CandleSource;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.csv.config.CsvConfiguration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CandlesCsvRepositoryTest extends BaseCsvRepositoryTest<Candle> {

  @SneakyThrows
  @Override
  protected CsvRepository<Candle> createRepository(CsvConfiguration config) {
    return new CandlesCsvRepository(config);
  }

  @Override
  protected Candle createEntity(String instrumentUid) {
    return Candle.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(LocalDateTime.now()))
      .setInstrumentUid(instrumentUid)
      .setInterval(SubscriptionInterval.SUBSCRIPTION_INTERVAL_2_MIN)
      .setOpen(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(10.0)))
      .setHigh(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(10.0)))
      .setLow(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(10.0)))
      .setClose(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(10.0)))
      .setVolume(1000000L)
      .setLastTradeTs(TimeMapper.localDateTimeToTimestamp(LocalDateTime.now()))
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
