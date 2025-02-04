package ru.ttech.piapi.storage.jdbc.repository;

import ru.tinkoff.piapi.contract.v1.SecurityTradingStatus;
import ru.tinkoff.piapi.contract.v1.TradingStatus;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.storage.jdbc.config.JdbcConfiguration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TradingStatusesJdbcRepository extends JdbcRepository<TradingStatus> {

  public TradingStatusesJdbcRepository(JdbcConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String getTableQuery() {
    return "CREATE TABLE IF NOT EXISTS " + getTableName() + " (" +
      "c_time TIMESTAMP(6), " +
      "c_instrument_uid VARCHAR(255), " +
      "c_trading_status TEXT, " +
      "c_limit_order_available_flag BOOLEAN, " +
      "c_market_order_available_flag BOOLEAN, " +
      "PRIMARY KEY (c_time, c_instrument_uid))";
  }

  @Override
  protected String getInsertQuery() {
    return "INSERT INTO " + getTableName() + " (" +
      "c_time, c_instrument_uid, c_trading_status, c_limit_order_available_flag, " +
      "c_market_order_available_flag) VALUES (?, ?, ?, ?, ?)";
  }

  @Override
  protected TradingStatus parseEntityFromResultSet(ResultSet rs) throws SQLException {
    return TradingStatus.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(rs.getTimestamp(1).toLocalDateTime()))
      .setInstrumentUid(rs.getString(2))
      .setTradingStatus(SecurityTradingStatus.valueOf(rs.getString(3)))
      .setLimitOrderAvailableFlag(rs.getBoolean(4))
      .setMarketOrderAvailableFlag(rs.getBoolean(5))
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
