package ru.ttech.piapi.storage.jdbc.repository;

import com.google.protobuf.Timestamp;
import com.mysql.cj.jdbc.MysqlDataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.postgresql.ds.PGSimpleDataSource;
import ru.ttech.piapi.storage.jdbc.config.JdbcConfiguration;

import javax.sql.DataSource;
import java.time.Instant;

public abstract class BaseIntegrationTest {

  protected static PGSimpleDataSource pgDataSource;
  protected static MysqlDataSource mysqlDataSource;

  @SneakyThrows
  @BeforeAll
  static void setUp() {
    Containers.POSTGRES.start();
    Containers.MYSQL.start();
    pgDataSource = new PGSimpleDataSource();
    pgDataSource.setUrl(Containers.POSTGRES.getJdbcUrl());
    pgDataSource.setUser(Containers.POSTGRES.getUsername());
    pgDataSource.setPassword(Containers.POSTGRES.getPassword());
    mysqlDataSource = new MysqlDataSource();
    mysqlDataSource.setUrl(Containers.MYSQL.getJdbcUrl());
    mysqlDataSource.setUser(Containers.MYSQL.getUsername());
    mysqlDataSource.setPassword(Containers.MYSQL.getPassword());
  }

  protected final JdbcConfiguration createJdbcConfiguration(DataSource dataSource, String tableName) {
    return new JdbcConfiguration(dataSource, null, tableName);
  }

  protected static Timestamp getTimestampFromInstant(Instant instant) {
    return Timestamp.newBuilder()
      .setSeconds(instant.getEpochSecond())
      .setNanos(instant.getNano() - instant.getNano() % 1000)
      .build();
  }

  @AfterAll
  static void tearDown() {
    Containers.POSTGRES.stop();
    Containers.MYSQL.stop();
  }
}
