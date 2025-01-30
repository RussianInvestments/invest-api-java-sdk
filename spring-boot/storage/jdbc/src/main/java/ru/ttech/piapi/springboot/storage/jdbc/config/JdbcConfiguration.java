package ru.ttech.piapi.springboot.storage.jdbc.config;

public class JdbcConfiguration {
  private final String url;
  private final String username;
  private final String password;
  private final String tableName;

  public JdbcConfiguration(String url, String username, String password, String tableName) {
    this.url = url;
    this.username = username;
    this.password = password;
    this.tableName = tableName;
  }

  public String getUrl() {
    return url;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getTableName() {
    return tableName;
  }
}
