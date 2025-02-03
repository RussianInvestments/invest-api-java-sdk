package ru.ttech.piapi.storage.jdbc.repository;

import com.google.common.base.Splitter;
import io.vavr.collection.Stream;
import ru.tinkoff.piapi.contract.v1.Order;
import ru.tinkoff.piapi.contract.v1.OrderBook;
import ru.tinkoff.piapi.contract.v1.OrderBookType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.storage.jdbc.config.JdbcConfiguration;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OrderBooksJdbcRepository extends JdbcRepository<OrderBook> {

  public OrderBooksJdbcRepository(JdbcConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String getTableQuery() {
    return "CREATE TABLE IF NOT EXISTS " + getTableName() + " (" +
      "c_time TIMESTAMP(6), " +
      "c_instrument_uid VARCHAR(255), " +
      "c_bids_prices TEXT," +
      "c_bids_vols TEXT, " +
      "c_asks_prices TEXT, " +
      "c_asks_vols TEXT, " +
      "c_is_consistent BOOLEAN, " +
      "c_depth INTEGER, " +
      "c_limit_up DECIMAL(19, 4), " +
      "c_limit_down DECIMAL(19, 4), " +
      "c_order_book_type TEXT, " +
      "PRIMARY KEY (c_time, c_instrument_uid))";
  }

  @Override
  protected String getInsertQuery() {
    return "INSERT INTO " + getTableName() + " (" +
      "c_time, c_instrument_uid, c_bids_prices, c_bids_vols, c_asks_prices, c_asks_vols, c_is_consistent, " +
      "c_depth, c_limit_up, c_limit_down, c_order_book_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
  }

  @Override
  protected OrderBook parseEntityFromResultSet(ResultSet rs) throws SQLException {
    return OrderBook.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(rs.getTimestamp(1).toLocalDateTime()))
      .setInstrumentUid(rs.getString(2))
      .addAllBids(buildOrders(rs.getString(3), rs.getString(4)))
      .addAllAsks(buildOrders(rs.getString(5), rs.getString(6)))
      .setIsConsistent(rs.getBoolean(7))
      .setDepth(rs.getInt(8))
      .setLimitUp(NumberMapper.bigDecimalToQuotation(rs.getBigDecimal(9)))
      .setLimitDown(NumberMapper.bigDecimalToQuotation(rs.getBigDecimal(10)))
      .setOrderBookType(OrderBookType.valueOf(rs.getString(11)))
      .build();
  }

  @Override
  protected void setStatementParameters(PreparedStatement stmt, OrderBook entity) throws SQLException {
    stmt.setTimestamp(1, Timestamp.valueOf(TimeMapper.timestampToLocalDateTime(entity.getTime())));
    stmt.setString(2, entity.getInstrumentUid());
    stmt.setString(3, getValuesFromOrders(entity.getBidsList(),
      order -> NumberMapper.quotationToBigDecimal(order.getPrice())).toString());
    stmt.setString(4, getValuesFromOrders(entity.getBidsList(), Order::getQuantity).toString());
    stmt.setString(5, getValuesFromOrders(entity.getAsksList(),
      order -> NumberMapper.quotationToBigDecimal(order.getPrice())).toString());
    stmt.setString(6, getValuesFromOrders(entity.getAsksList(), Order::getQuantity).toString());
    stmt.setBoolean(7, entity.getIsConsistent());
    stmt.setInt(8, entity.getDepth());
    stmt.setBigDecimal(9, NumberMapper.quotationToBigDecimal(entity.getLimitUp()));
    stmt.setBigDecimal(10, NumberMapper.quotationToBigDecimal(entity.getLimitDown()));
    stmt.setString(11, entity.getOrderBookType().name());
  }

  private Order buildOrder(BigDecimal price, Long quantity) {
    return Order.newBuilder()
      .setPrice(NumberMapper.bigDecimalToQuotation(price))
      .setQuantity(quantity)
      .build();
  }

  private Iterable<Order> buildOrders(String pricesArray, String volsArray) {
    var prices = parseValues(pricesArray, BigDecimal::new);
    var quantities = parseValues(volsArray, Long::parseLong);
    return prices.zip(quantities).map(order -> buildOrder(order._1(), order._2()))
      .collect(Collectors.toUnmodifiableList());
  }

  private Iterable<?> getValuesFromOrders(List<Order> orders, Function<Order, ?> mapper) {
    return orders.stream().map(mapper).collect(Collectors.toUnmodifiableList());
  }

  private <T> Stream<T> parseValues(String array, Function<String, T> convertor) {
    return Stream.ofAll(parseElements(array)).map(convertor);
  }

  private Iterable<String> parseElements(String array) {
    return Splitter.on(", ").split(array.substring(1, array.length() - 1));
  }
}
