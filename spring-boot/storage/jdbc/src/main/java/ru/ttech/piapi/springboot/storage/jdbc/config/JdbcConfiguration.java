package ru.ttech.piapi.springboot.storage.jdbc.config;

import javax.sql.DataSource;

public class JdbcConfiguration {
  private final String tableName;
  private final DataSource dataSource;

  public JdbcConfiguration(DataSource dataSource, String tableName) {
    this.dataSource = dataSource;
    this.tableName = tableName;
  }

  public String getTableName() {
    return tableName;
  }

  public DataSource getDataSource() {
    return dataSource;
  }
}
