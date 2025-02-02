package ru.ttech.piapi.springboot.storage.csv.repository;

import com.google.protobuf.Timestamp;
import lombok.SneakyThrows;
import ru.tinkoff.piapi.contract.v1.Order;
import ru.tinkoff.piapi.contract.v1.OrderBook;
import ru.tinkoff.piapi.contract.v1.OrderBookType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.csv.config.CsvConfiguration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OrderBooksCsvRepositoryTest extends BaseCsvRepositoryTest<OrderBook> {

  @SneakyThrows
  @Override
  protected CsvRepository<OrderBook> createRepository(CsvConfiguration config) {
    return new OrderBooksCsvRepository(config);
  }

  @Override
  protected OrderBook createEntity() {
    var order = Order.newBuilder()
      .setPrice(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(12.67)))
      .setQuantity(10)
      .build();
    return OrderBook.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(LocalDateTime.now()))
      .setInstrumentUid(UUID.randomUUID().toString())
      .addAllBids(List.of(order, order))
      .addAllAsks(List.of(order, order))
      .setIsConsistent(false)
      .setDepth(15)
      .setLimitUp(NumberMapper.bigDecimalToQuotation(BigDecimal.TEN))
      .setLimitDown(NumberMapper.bigDecimalToQuotation(BigDecimal.ONE))
      .setOrderBookType(OrderBookType.ORDERBOOK_TYPE_EXCHANGE)
      .build();
  }

  @Override
  protected Timestamp getEntityTime(OrderBook entity) {
    return entity.getTime();
  }

  @Override
  protected String getEntityInstrumentUid(OrderBook entity) {
    return entity.getInstrumentUid();
  }
}
