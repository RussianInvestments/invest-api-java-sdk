package ru.ttech.piapi.springboot.storage.jdbc.repository;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.BeforeAll;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.ttech.piapi.springboot.storage.jdbc.config.JdbcConfiguration;

import javax.sql.DataSource;
import java.time.Instant;

@Testcontainers
public abstract class BaseIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  protected static PGSimpleDataSource postgresDataSource;

  @BeforeAll
  static void setUp() {
    postgresDataSource = new PGSimpleDataSource();
    postgresDataSource.setUrl(postgres.getJdbcUrl());
    postgresDataSource.setUser(postgres.getUsername());
    postgresDataSource.setPassword(postgres.getPassword());
  }

  protected final JdbcConfiguration createJdbcConfiguration(DataSource dataSource) {
    return new JdbcConfiguration(dataSource, null, "default_name");
  }

  protected static Timestamp getTimestampNow() {
    var time = Instant.now();
    return Timestamp.newBuilder()
      .setSeconds(time.getEpochSecond())
      .setNanos(time.getNano() - time.getNano() % 1000)
      .build();
  }
}
