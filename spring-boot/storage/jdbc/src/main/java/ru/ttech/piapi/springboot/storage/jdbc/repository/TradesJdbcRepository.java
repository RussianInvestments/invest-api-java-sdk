package ru.ttech.piapi.springboot.storage.jdbc.repository;

import ru.tinkoff.piapi.contract.v1.Trade;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.jdbc.config.JdbcConfiguration;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TradesJdbcRepository extends JdbcRepository<Trade> {

  public TradesJdbcRepository(JdbcConfiguration configuration) throws SQLException {
    super(configuration);
  }

  @Override
  protected String getTableSchema() {
    return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
      "time TIMESTAMP, " +
      "instrument_uid TEXT, " +
      "direction TEXT, " +
      "price DECIMAL(19,4), " +
      "quantity BIGINT, " +
      "trade_source TEXT" +
      ")";
  }

  @Override
  protected String getInsertQuery() {
    return "INSERT INTO " + tableName + " (" +
      "time, instrument_uid, direction, price, quantity, trade_source) VALUES (?, ?, ?, ?, ?, ?)";
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
