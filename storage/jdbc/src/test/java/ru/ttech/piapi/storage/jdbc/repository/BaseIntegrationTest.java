package ru.ttech.piapi.storage.jdbc.repository;

import com.google.protobuf.Timestamp;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.ttech.piapi.storage.jdbc.config.JdbcConfiguration;

import javax.sql.DataSource;
import java.time.Instant;

@Testcontainers
public abstract class BaseIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:9.2.0");

  protected static PGSimpleDataSource pgDataSource;
  protected static MysqlDataSource mysqlDataSource;

  @BeforeAll
  static void setUp() {
    pgDataSource = new PGSimpleDataSource();
    pgDataSource.setUrl(postgres.getJdbcUrl());
    pgDataSource.setUser(postgres.getUsername());
    pgDataSource.setPassword(postgres.getPassword());
    mysqlDataSource = new MysqlDataSource();
    mysqlDataSource.setUrl(mysql.getJdbcUrl());
    mysqlDataSource.setUser(mysql.getUsername());
    mysqlDataSource.setPassword(mysql.getPassword());
  }

  protected final JdbcConfiguration createJdbcConfiguration(DataSource dataSource) {
    return new JdbcConfiguration(dataSource, null, "default_name");
  }

  protected static Timestamp getTimestampFromInstant(Instant instant) {
    return Timestamp.newBuilder()
      .setSeconds(instant.getEpochSecond())
      .setNanos(instant.getNano() - instant.getNano() % 1000)
      .build();
  }
}
