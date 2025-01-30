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
      "figi TEXT, " +
      "interval TEXT, " +
      "open_price DECIMAL(19,4), " +
      "high_price DECIMAL(19,4), " +
      "low_price DECIMAL(19,4), " +
      "close_price DECIMAL(19,4), " +
      "volume BIGINT, " +
      "time TIMESTAMP, " +
      "last_trade_ts TIMESTAMP, " +
      "instrument_uid TEXT, " +
      "candle_source_type TEXT" +
      ")";
  }

  @Override
  protected String getInsertQuery() {
    return "INSERT INTO " + tableName + " (" +
      "figi, interval, open_price, high_price, low_price, close_price, volume, time, last_trade_ts, " +
      "instrument_uid, candle_source_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
  }

  @Override
  protected void setStatementParameters(PreparedStatement stmt, Candle candle) throws SQLException {
    stmt.setString(1, candle.getFigi());
    stmt.setString(2, candle.getInterval().name());
    stmt.setBigDecimal(3, NumberMapper.quotationToBigDecimal(candle.getOpen()));
    stmt.setBigDecimal(4, NumberMapper.quotationToBigDecimal(candle.getHigh()));
    stmt.setBigDecimal(5, NumberMapper.quotationToBigDecimal(candle.getLow()));
    stmt.setBigDecimal(6, NumberMapper.quotationToBigDecimal(candle.getClose()));
    stmt.setLong(7, candle.getVolume());
    stmt.setTimestamp(8, Timestamp.valueOf(TimeMapper.timestampToLocalDateTime(candle.getTime())));
    stmt.setTimestamp(9, Timestamp.valueOf(TimeMapper.timestampToLocalDateTime(candle.getLastTradeTs())));
    stmt.setString(10, candle.getInstrumentUid());
    stmt.setString(11, candle.getCandleSourceType().name());
  }
}
