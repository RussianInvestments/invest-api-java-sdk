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

public class OrderBooksCsvRepositoryTest extends BaseCsvRepositoryTest<OrderBook> {

  @SneakyThrows
  @Override
  protected CsvRepository<OrderBook> createRepository(CsvConfiguration config) {
    return new OrderBooksCsvRepository(config);
  }

  @Override
  protected OrderBook createEntity(String instrumentUid) {
    var order = Order.newBuilder()
      .setPrice(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(12.67)))
      .setQuantity(10)
      .build();
    var orderTwo = Order.newBuilder()
      .setPrice(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(13.96)))
      .setQuantity(30)
      .build();
    var orderThree = Order.newBuilder()
      .setPrice(NumberMapper.bigDecimalToQuotation(BigDecimal.valueOf(14.07)))
      .setQuantity(24)
      .build();
    return OrderBook.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(LocalDateTime.now()))
      .setInstrumentUid(instrumentUid)
      .addAllBids(List.of(order, orderTwo))
      .addAllAsks(List.of(orderThree, orderTwo, order))
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
