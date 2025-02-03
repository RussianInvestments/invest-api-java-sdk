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
      "time TIMESTAMP, " +
      "instrument_uid TEXT, " +
      "trading_status TEXT, " +
      "limit_order_available_flag BOOLEAN, " +
      "market_order_available_flag BOOLEAN, " +
      "PRIMARY KEY (time, instrument_uid))";
  }

  @Override
  protected String getInsertQuery() {
    return "INSERT INTO " + getTableName() + " (" +
      "time, instrument_uid, trading_status, limit_order_available_flag, " +
      "market_order_available_flag) VALUES (?, ?, ?, ?, ?)";
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
