package ru.ttech.piapi.springboot.storage.jdbc.repository;

import io.vavr.collection.Stream;
import ru.tinkoff.piapi.contract.v1.Order;
import ru.tinkoff.piapi.contract.v1.OrderBook;
import ru.tinkoff.piapi.contract.v1.OrderBookType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.jdbc.config.JdbcConfiguration;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.function.Function;

public class OrderBooksJdbcRepository extends JdbcRepository<OrderBook> {

  public OrderBooksJdbcRepository(JdbcConfiguration configuration) throws SQLException {
    super(configuration);
  }

  @Override
  protected String getTableSchema() {
    return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
      "time TIMESTAMP, " +
      "instrument_uid TEXT, " +
      "bids_prices DECIMAL(19, 4)[]," +
      "bids_vols BIGINT[], " +
      "asks_prices DECIMAL(19, 4)[], " +
      "asks_vols BIGINT[], " +
      "is_consistent BOOLEAN, " +
      "depth INTEGER, " +
      "limit_up DECIMAL(19, 4), " +
      "limit_down DECIMAL(19, 4), " +
      "order_book_type TEXT" +
      ")";
  }

  @Override
  protected String getInsertQuery() {
    return "INSERT INTO " + tableName + " (" +
      "time, instrument_uid, bids_prices, bids_vols, asks_prices, asks_vols, is_consistent, depth, limit_up, " +
      "limit_down, order_book_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
  }

  @Override
  protected OrderBook parseEntityFromResultSet(ResultSet rs) throws SQLException {
    var bidsPrices = (BigDecimal[]) rs.getArray(3).getArray();
    var bidsVols = (Long[]) rs.getArray(4).getArray();
    var asksPrices = (BigDecimal[]) rs.getArray(5).getArray();
    var asksVols = (Long[]) rs.getArray(6).getArray();
    return OrderBook.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(rs.getTimestamp(1).toLocalDateTime()))
      .setInstrumentUid(rs.getString(2))
      .addAllBids(buildOrders(bidsPrices, bidsVols))
      .addAllAsks(buildOrders(asksPrices, asksVols))
      .setIsConsistent(rs.getBoolean(7))
      .setDepth(rs.getInt(8))
      .setLimitUp(NumberMapper.bigDecimalToQuotation(rs.getBigDecimal(9)))
      .setLimitDown(NumberMapper.bigDecimalToQuotation(rs.getBigDecimal(10)))
      .setOrderBookType(OrderBookType.valueOf(rs.getString(11)))
      .build();
  }

  @Override
  protected void setStatementParameters(PreparedStatement stmt, OrderBook entity) throws SQLException {
    var bidPrices = getValuesFromOrders(entity.getBidsList(),
      order -> NumberMapper.quotationToBigDecimal(order.getPrice()));
    var askPrices = getValuesFromOrders(entity.getAsksList(),
      order -> NumberMapper.quotationToBigDecimal(order.getPrice()));
    var bidQuantities = getValuesFromOrders(entity.getBidsList(), Order::getQuantity);
    var askQuantities = getValuesFromOrders(entity.getAsksList(), Order::getQuantity);
    stmt.setTimestamp(1, Timestamp.valueOf(TimeMapper.timestampToLocalDateTime(entity.getTime())));
    stmt.setString(2, entity.getInstrumentUid());
    stmt.setArray(3, connection.createArrayOf("decimal", bidPrices));
    stmt.setArray(4, connection.createArrayOf("bigint", bidQuantities));
    stmt.setArray(5, connection.createArrayOf("decimal", askPrices));
    stmt.setArray(6, connection.createArrayOf("bigint", askQuantities));
    stmt.setBoolean(7, entity.getIsConsistent());
    stmt.setInt(8, entity.getDepth());
    stmt.setBigDecimal(9, NumberMapper.quotationToBigDecimal(entity.getLimitUp()));
    stmt.setBigDecimal(10, NumberMapper.quotationToBigDecimal(entity.getLimitDown()));
    stmt.setString(11, entity.getOrderBookType().name());
  }

  private Object[] getValuesFromOrders(List<Order> orders, Function<Order, ?> mapper) {
    return orders.stream().map(mapper).toArray(Object[]::new);
  }

  private Iterable<Order> buildOrders(BigDecimal[] prices, Long[] quantities) {
    return Stream.of(prices).zip(Stream.of(quantities))
      .map(order -> buildOrder(order._1(), order._2()));
  }

  private Order buildOrder(BigDecimal price, Long quantity) {
    return Order.newBuilder()
      .setPrice(NumberMapper.bigDecimalToQuotation(price))
      .setQuantity(quantity)
      .build();
  }
}
