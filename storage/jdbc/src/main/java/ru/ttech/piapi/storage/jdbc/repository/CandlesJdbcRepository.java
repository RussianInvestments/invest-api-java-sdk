package ru.ttech.piapi.storage.jdbc.repository;

import ru.tinkoff.piapi.contract.v1.Candle;
import ru.tinkoff.piapi.contract.v1.CandleSource;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.storage.jdbc.config.JdbcConfiguration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class CandlesJdbcRepository extends JdbcRepository<Candle> {

  public CandlesJdbcRepository(JdbcConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String getTableQuery() {
    return "CREATE TABLE IF NOT EXISTS " + getTableName() + " (" +
      "c_time TIMESTAMP(6), " +
      "c_instrument_uid VARCHAR(255), " +
      "c_interval TEXT, " +
      "c_open_price DECIMAL(19,4), " +
      "c_high_price DECIMAL(19,4), " +
      "c_low_price DECIMAL(19,4), " +
      "c_close_price DECIMAL(19,4), " +
      "c_volume BIGINT, " +
      "c_last_trade_ts TIMESTAMP(6), " +
      "c_candle_source_type TEXT, " +
      "PRIMARY KEY (c_time, c_instrument_uid, c_last_trade_ts))";
  }

  @Override
  protected String getInsertQuery() {
    return "INSERT INTO " + getTableName() + " (" +
      "c_time, c_instrument_uid, c_interval, c_open_price, c_high_price, c_low_price, c_close_price, c_volume, c_last_trade_ts, " +
      "c_candle_source_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
  }

  @Override
  protected Candle parseEntityFromResultSet(ResultSet rs) throws SQLException {
    return Candle.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(rs.getTimestamp(1).toLocalDateTime()))
      .setInstrumentUid(rs.getString(2))
      .setInterval(SubscriptionInterval.valueOf(rs.getString(3)))
      .setOpen(NumberMapper.bigDecimalToQuotation(rs.getBigDecimal(4)))
      .setHigh(NumberMapper.bigDecimalToQuotation(rs.getBigDecimal(5)))
      .setLow(NumberMapper.bigDecimalToQuotation(rs.getBigDecimal(6)))
      .setClose(NumberMapper.bigDecimalToQuotation(rs.getBigDecimal(7)))
      .setVolume(rs.getLong(8))
      .setLastTradeTs(TimeMapper.localDateTimeToTimestamp(rs.getTimestamp(9).toLocalDateTime()))
      .setCandleSourceType(CandleSource.valueOf(rs.getString(10)))
      .build();
  }

  @Override
  protected void setStatementParameters(PreparedStatement stmt, Candle candle) throws SQLException {
    stmt.setTimestamp(1, Timestamp.valueOf(TimeMapper.timestampToLocalDateTime(candle.getTime())));
    stmt.setString(2, candle.getInstrumentUid());
    stmt.setString(3, candle.getInterval().name());
    stmt.setBigDecimal(4, NumberMapper.quotationToBigDecimal(candle.getOpen()));
    stmt.setBigDecimal(5, NumberMapper.quotationToBigDecimal(candle.getHigh()));
    stmt.setBigDecimal(6, NumberMapper.quotationToBigDecimal(candle.getLow()));
    stmt.setBigDecimal(7, NumberMapper.quotationToBigDecimal(candle.getClose()));
    stmt.setLong(8, candle.getVolume());
    stmt.setTimestamp(9, Timestamp.valueOf(TimeMapper.timestampToLocalDateTime(candle.getLastTradeTs())));
    stmt.setString(10, candle.getCandleSourceType().name());
  }
}
