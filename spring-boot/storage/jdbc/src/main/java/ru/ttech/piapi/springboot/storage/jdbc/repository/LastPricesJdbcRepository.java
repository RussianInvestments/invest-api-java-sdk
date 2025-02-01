package ru.ttech.piapi.springboot.storage.jdbc.repository;

import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.LastPriceType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.jdbc.config.JdbcConfiguration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class LastPricesJdbcRepository extends JdbcRepository<LastPrice> {

  public LastPricesJdbcRepository(JdbcConfiguration configuration) throws SQLException {
    super(configuration);
  }

  @Override
  protected String getTableQuery() {
    return "CREATE TABLE IF NOT EXISTS " + getTableName() + " (" +
      "time TIMESTAMP, " +
      "instrument_uid TEXT, " +
      "price DECIMAL(19,4), " +
      "last_price_type TEXT, " +
      "PRIMARY KEY (time, instrument_uid))";
  }

  @Override
  protected String getInsertQuery() {
    return "INSERT INTO " + getTableName() + " (time, instrument_uid, figi, price, last_price_type) VALUES (?, ?, ?, ?)";
  }

  @Override
  protected LastPrice parseEntityFromResultSet(ResultSet rs) throws SQLException {
    return LastPrice.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(rs.getTimestamp("time").toLocalDateTime()))
      .setInstrumentUid(rs.getString("instrument_uid"))
      .setPrice(NumberMapper.bigDecimalToQuotation(rs.getBigDecimal("price")))
      .setLastPriceType(LastPriceType.valueOf(rs.getString("last_price_type")))
      .build();
  }

  @Override
  protected void setStatementParameters(PreparedStatement stmt, LastPrice entity) throws SQLException {
    stmt.setTimestamp(1, Timestamp.valueOf(TimeMapper.timestampToLocalDateTime(entity.getTime())));
    stmt.setString(2, entity.getInstrumentUid());
    stmt.setBigDecimal(3, NumberMapper.quotationToBigDecimal(entity.getPrice()));
    stmt.setString(4, entity.getLastPriceType().name());
  }
}
