package ru.ttech.piapi.storage.jdbc.repository;

import com.google.protobuf.Timestamp;
import lombok.SneakyThrows;
import ru.tinkoff.piapi.contract.v1.Order;
import ru.tinkoff.piapi.contract.v1.OrderBook;
import ru.tinkoff.piapi.contract.v1.OrderBookType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.storage.jdbc.config.JdbcConfiguration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderBooksJdbcRepositoryTest extends BaseJdbcRepositoryTest<OrderBook, OrderBooksJdbcRepository> {

  @SneakyThrows
  @Override
  protected OrderBooksJdbcRepository createRepository(JdbcConfiguration configuration) {
    return new OrderBooksJdbcRepository(configuration);
  }

  @Override
  protected OrderBook createEntity(String instrumentUid, Instant instant) {
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
      .setTime(getTimestampFromInstant(instant))
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
