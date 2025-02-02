package ru.ttech.piapi.springboot.storage.jdbc.repository;

import ru.tinkoff.piapi.contract.v1.Trade;
import ru.tinkoff.piapi.contract.v1.TradeDirection;
import ru.tinkoff.piapi.contract.v1.TradeSourceType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.jdbc.config.JdbcConfiguration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TradesJdbcRepository extends JdbcRepository<Trade> {

  public TradesJdbcRepository(JdbcConfiguration configuration) throws SQLException {
    super(configuration);
  }

  @Override
  protected String getTableQuery() {
    return "CREATE TABLE IF NOT EXISTS " + getTableName() + " (" +
      "time TIMESTAMP, " +
      "instrument_uid TEXT, " +
      "direction TEXT, " +
      "price DECIMAL(19,4), " +
      "quantity BIGINT, " +
      "trade_source TEXT, " +
      "PRIMARY KEY (time, instrument_uid))";
  }

  @Override
  protected String getInsertQuery() {
    return "INSERT INTO " + getTableName() + " (" +
      "time, instrument_uid, direction, price, quantity, trade_source) VALUES (?, ?, ?, ?, ?, ?)";
  }

  @Override
  protected Trade parseEntityFromResultSet(ResultSet rs) throws SQLException {
    return Trade.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(rs.getTimestamp(1).toLocalDateTime()))
      .setInstrumentUid(rs.getString(2))
      .setDirection(TradeDirection.valueOf(rs.getString(3)))
      .setPrice(NumberMapper.bigDecimalToQuotation(rs.getBigDecimal(4)))
      .setQuantity(rs.getLong(5))
      .setTradeSource(TradeSourceType.valueOf(rs.getString(6)))
      .build();
  }

  @Override
  protected void setStatementParameters(PreparedStatement stmt, Trade entity) throws SQLException {
    stmt.setTimestamp(1, Timestamp.valueOf(TimeMapper.timestampToLocalDateTime(entity.getTime())));
    stmt.setString(2, entity.getInstrumentUid());
    stmt.setString(3, entity.getDirection().name());
    stmt.setBigDecimal(4, NumberMapper.quotationToBigDecimal(entity.getPrice()));
    stmt.setLong(5, entity.getQuantity());
    stmt.setString(6, entity.getTradeSource().name());
  }
}
