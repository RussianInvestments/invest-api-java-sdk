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
      "figi TEXT, " +
      "direction TEXT, " +
      "price DECIMAL(19,4), " +
      "quantity BIGINT, " +
      "time TIMESTAMP, " +
      "instrument_uid TEXT, " +
      "trade_source TEXT" +
      ")";
  }

  @Override
  protected String getInsertQuery() {
    return "INSERT INTO " + tableName + " (" +
      "figi, direction, price, quantity, time, instrument_uid, trade_source" +
      "VALUES (?, ?, ?, ?, ?, ?, ?)";
  }

  @Override
  protected void setStatementParameters(PreparedStatement stmt, Trade entity) throws SQLException {
    stmt.setString(1, entity.getFigi());
    stmt.setString(2, entity.getDirection().name());
    stmt.setBigDecimal(3, NumberMapper.quotationToBigDecimal(entity.getPrice()));
    stmt.setLong(4, entity.getQuantity());
    stmt.setTimestamp(5, Timestamp.valueOf(TimeMapper.timestampToLocalDateTime(entity.getTime())));
    stmt.setString(6, entity.getInstrumentUid());
    stmt.setString(7, entity.getTradeSource().name());
  }
}
