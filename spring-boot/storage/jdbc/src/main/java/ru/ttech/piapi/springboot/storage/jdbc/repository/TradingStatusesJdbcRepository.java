package ru.ttech.piapi.springboot.storage.jdbc.repository;

import ru.tinkoff.piapi.contract.v1.SecurityTradingStatus;
import ru.tinkoff.piapi.contract.v1.TradingStatus;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.jdbc.config.JdbcConfiguration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TradingStatusesJdbcRepository extends JdbcRepository<TradingStatus> {

  public TradingStatusesJdbcRepository(JdbcConfiguration configuration) throws SQLException {
    super(configuration);
  }

  @Override
  protected String getTableSchema() {
    return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
      "time TIMESTAMP, " +
      "instrument_uid TEXT, " +
      "trading_status TEXT, " +
      "limit_order_available_flag BOOLEAN, " +
      "market_order_available_flag BOOLEAN" +
      ")";
  }

  @Override
  protected String getInsertQuery() {
    return "INSERT INTO " + tableName + " (" +
      "time, instrument_uid, trading_status, limit_order_available_flag, " +
      "market_order_available_flag) VALUES (?, ?, ?, ?, ?)";
  }

  @Override
  protected TradingStatus parseEntityFromResultSet(ResultSet rs) throws SQLException {
    return TradingStatus.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(rs.getTimestamp("time").toLocalDateTime()))
      .setInstrumentUid(rs.getString("instrument_uid"))
      .setTradingStatus(SecurityTradingStatus.valueOf(rs.getString("trading_status")))
      .setLimitOrderAvailableFlag(rs.getBoolean("limit_order_available_flag"))
      .setMarketOrderAvailableFlag(rs.getBoolean("market_order_available_flag"))
      .build();
  }

  @Override
  protected void setStatementParameters(PreparedStatement stmt, TradingStatus entity) throws SQLException {
    stmt.setTimestamp(1, Timestamp.valueOf(TimeMapper.timestampToLocalDateTime(entity.getTime())));
    stmt.setString(2, entity.getInstrumentUid());
    stmt.setString(3, entity.getTradingStatus().name());
    stmt.setBoolean(4, entity.getLimitOrderAvailableFlag());
    stmt.setBoolean(5, entity.getMarketOrderAvailableFlag());
  }
}
