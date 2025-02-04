package ru.ttech.piapi.storage.jdbc.repository;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OrderBooksJdbcRepository extends JdbcRepository<OrderBook> {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public OrderBooksJdbcRepository(JdbcConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String getTableQuery() {
    return "CREATE TABLE IF NOT EXISTS " + getTableName() + " (" +
      "c_time TIMESTAMP(6), " +
      "c_instrument_uid VARCHAR(255), " +
      "c_bids TEXT, " +
      "c_asks TEXT, " +
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
      "c_time, c_instrument_uid, c_bids, c_asks, c_is_consistent, " +
      "c_depth, c_limit_up, c_limit_down, c_order_book_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
  }

  @Override
  protected OrderBook parseEntityFromResultSet(ResultSet rs) throws SQLException {
    try {
      return OrderBook.newBuilder()
        .setTime(TimeMapper.localDateTimeToTimestamp(rs.getTimestamp(1).toLocalDateTime()))
        .setInstrumentUid(rs.getString(2))
        .addAllBids(parseOrders(rs.getString(3)))
        .addAllAsks(parseOrders(rs.getString(4)))
        .setIsConsistent(rs.getBoolean(5))
        .setDepth(rs.getInt(6))
        .setLimitUp(NumberMapper.bigDecimalToQuotation(rs.getBigDecimal(7)))
        .setLimitDown(NumberMapper.bigDecimalToQuotation(rs.getBigDecimal(8)))
        .setOrderBookType(OrderBookType.valueOf(rs.getString(9)))
        .build();
    } catch (JsonProcessingException e) {
      throw new SQLException("Failed to parse JSON orders", e);
    }
  }

  @Override
  protected void setStatementParameters(PreparedStatement stmt, OrderBook entity) throws SQLException {
    try {
      stmt.setTimestamp(1, Timestamp.valueOf(TimeMapper.timestampToLocalDateTime(entity.getTime())));
      stmt.setString(2, entity.getInstrumentUid());
      stmt.setString(3, convertOrdersToJson(entity.getBidsList()));
      stmt.setString(4, convertOrdersToJson(entity.getAsksList()));
      stmt.setBoolean(5, entity.getIsConsistent());
      stmt.setInt(6, entity.getDepth());
      stmt.setBigDecimal(7, NumberMapper.quotationToBigDecimal(entity.getLimitUp()));
      stmt.setBigDecimal(8, NumberMapper.quotationToBigDecimal(entity.getLimitDown()));
      stmt.setString(9, entity.getOrderBookType().name());
    } catch (JsonProcessingException e) {
      throw new SQLException("Failed to serialize orders to JSON", e);
    }
  }

  private List<Order> parseOrders(String jsonOrders) throws JsonProcessingException {
    if (jsonOrders == null || jsonOrders.isEmpty()) {
      return Collections.emptyList();
    }
    var orderPojoList = objectMapper.readValue(jsonOrders, new TypeReference<List<OrderPojo>>() {
    });
    return orderPojoList.stream()
      .map(this::convertPojoToOrder)
      .collect(Collectors.toList());
  }

  private String convertOrdersToJson(List<Order> orders) throws JsonProcessingException {
    List<OrderPojo> orderPojoList = orders.stream()
      .map(this::convertOrderToPojo)
      .collect(Collectors.toList());

    return objectMapper.writeValueAsString(orderPojoList);
  }

  private Order convertPojoToOrder(OrderPojo orderPojo) {
    return Order.newBuilder()
      .setPrice(NumberMapper.bigDecimalToQuotation(orderPojo.getPrice()))
      .setQuantity(orderPojo.getQuantity())
      .build();
  }

  private OrderPojo convertOrderToPojo(Order order) {
    return new OrderPojo(NumberMapper.quotationToBigDecimal(order.getPrice()), order.getQuantity());
  }

  private static class OrderPojo {

    private final BigDecimal price;
    private final Long quantity;

    @JsonCreator
    public OrderPojo(
      @JsonProperty("price") BigDecimal price,
      @JsonProperty("quantity") Long quantity
    ) {
      this.price = price;
      this.quantity = quantity;
    }

    public BigDecimal getPrice() {
      return price;
    }

    public Long getQuantity() {
      return quantity;
    }
  }
}
