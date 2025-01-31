package ru.ttech.piapi.springboot.storage.jdbc.repository;

import ru.tinkoff.piapi.contract.v1.Candle;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.jdbc.config.JdbcConfiguration;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class CandlesJdbcRepository extends JdbcRepository<Candle> {

  public CandlesJdbcRepository(JdbcConfiguration configuration) throws SQLException {
    super(configuration);
  }

  @Override
  protected String getTableSchema() {
    return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
      "time TIMESTAMP, " +
      "instrument_uid TEXT, " +
      "interval TEXT, " +
      "open_price DECIMAL(19,4), " +
      "high_price DECIMAL(19,4), " +
      "low_price DECIMAL(19,4), " +
      "close_price DECIMAL(19,4), " +
      "volume BIGINT, " +
      "last_trade_ts TIMESTAMP, " +
      "candle_source_type TEXT" +
      ")";
  }

  @Override
  protected String getInsertQuery() {
    return "INSERT INTO " + tableName + " (" +
      "time, instrument_uid, interval, open_price, high_price, low_price, close_price, volume, last_trade_ts, " +
      "candle_source_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
