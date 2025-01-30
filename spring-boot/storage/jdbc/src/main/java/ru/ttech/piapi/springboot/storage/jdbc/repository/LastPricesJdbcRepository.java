package ru.ttech.piapi.springboot.storage.jdbc.repository;

import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.jdbc.config.JdbcConfiguration;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class LastPricesJdbcRepository extends JdbcRepository<LastPrice> {

  public LastPricesJdbcRepository(JdbcConfiguration configuration) throws SQLException {
    super(configuration);
  }

  @Override
  protected String getTableSchema() {
    return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
      "figi TEXT, " +
      "price DECIMAL(19,4), " +
      "time TIMESTAMP, " +
      "instrument_uid TEXT, " +
      "last_price_type TEXT" +
      ")";
  }

  @Override
  protected String getInsertQuery() {
    return "INSERT INTO " + tableName + " (figi, price, time, instrument_uid, last_price_type) VALUES (?,?,?,?,?)";
  }

  @Override
  protected void setStatementParameters(PreparedStatement stmt, LastPrice entity) throws SQLException {
    stmt.setString(1, entity.getFigi());
    stmt.setBigDecimal(2, NumberMapper.quotationToBigDecimal(entity.getPrice()));
    stmt.setTimestamp(3, Timestamp.valueOf(TimeMapper.timestampToLocalDateTime(entity.getTime())));
    stmt.setString(4, entity.getInstrumentUid());
    stmt.setString(5, entity.getLastPriceType().name());
  }
}
