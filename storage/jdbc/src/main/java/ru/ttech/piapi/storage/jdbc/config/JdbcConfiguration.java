package ru.ttech.piapi.storage.jdbc.config;

import javax.sql.DataSource;

public class JdbcConfiguration {
  private final String tableName;
  private final String schemaName;
  private final DataSource dataSource;

  public JdbcConfiguration(DataSource dataSource, String schemaName, String tableName) {
    this.dataSource = dataSource;
    this.schemaName = schemaName;
    this.tableName = tableName;
  }

  public String getSchemaName() {
    return schemaName;
  }

  public String getTableName() {
    return tableName;
  }

  public DataSource getDataSource() {
    return dataSource;
  }
}
