package ru.ttech.piapi.springboot.storage.jdbc.repository;

import ru.tinkoff.piapi.contract.v1.TradingStatus;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.jdbc.config.JdbcConfiguration;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TradingStatusesJdbcRepository extends JdbcRepository<TradingStatus> {

  public TradingStatusesJdbcRepository(JdbcConfiguration configuration) throws SQLException {
    super(configuration);
  }

  @Override
  protected String getTableSchema() {
    return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
      "figi TEXT, " +
      "trading_status TEXT, " +
      "time TIMESTAMP, " +
      "limit_order_available_flag BOOLEAN, " +
      "market_order_available_flag BOOLEAN, " +
      "instrument_uid TEXT" +
      ")";
  }

  @Override
  protected String getInsertQuery() {
    return "INSERT INTO " + tableName + " (" +
      "figi, trading_status, time, limit_order_available_flag, market_order_available_flag," +
      " instrument_uid) VALUES (?,?,?,?,?,?)";
  }

  @Override
  protected void setStatementParameters(PreparedStatement stmt, TradingStatus entity) throws SQLException {
    stmt.setString(1, entity.getFigi());
    stmt.setString(2, entity.getTradingStatus().name());
    stmt.setTimestamp(3, Timestamp.valueOf(TimeMapper.timestampToLocalDateTime(entity.getTime())));
    stmt.setBoolean(4, entity.getLimitOrderAvailableFlag());
    stmt.setBoolean(5, entity.getMarketOrderAvailableFlag());
    stmt.setString(6, entity.getInstrumentUid());
  }
}
