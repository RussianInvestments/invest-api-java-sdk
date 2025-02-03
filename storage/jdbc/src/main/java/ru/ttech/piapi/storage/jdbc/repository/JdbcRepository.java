package ru.ttech.piapi.storage.jdbc.repository;

import ru.ttech.piapi.storage.core.repository.ReadWriteRepository;
import ru.ttech.piapi.storage.jdbc.config.JdbcConfiguration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public abstract class JdbcRepository<T> implements ReadWriteRepository<T> {

  protected final DataSource dataSource;
  protected final String tableName;
  protected final String schemaName;

  public JdbcRepository(JdbcConfiguration configuration) {
    this.dataSource = configuration.getDataSource();
    this.schemaName = configuration.getSchemaName();
    this.tableName = configuration.getTableName();
    createTableIfNotExists();
  }

  protected final String getTableName() {
    return Optional.ofNullable(schemaName)
      .map(schema -> String.format("%s.%s", schema, tableName))
      .orElse(tableName);
  }

  protected abstract String getTableQuery();

  protected abstract String getInsertQuery();

  protected abstract T parseEntityFromResultSet(ResultSet rs) throws SQLException;

  protected abstract void setStatementParameters(PreparedStatement stmt, T entity) throws SQLException;

  protected String getSchemaQuery() {
    return "CREATE SCHEMA IF NOT EXISTS " + schemaName;
  }

  protected String getFindAllQuery() {
    return "SELECT * FROM " + getTableName();
  }

  protected String getFindByTimeAndInstrumentUidQuery() {
    return "SELECT * FROM " + getTableName() + " WHERE time = ? AND instrument_uid = ?";
  }

  protected String getFindByPeriodAndInstrumentUidQuery() {
    return "SELECT * FROM " + getTableName() + " WHERE time BETWEEN ? AND ? AND instrument_uid = ?";
  }

  private <R> R executeInTransaction(SqlFunction<Connection, R> operation) {
    try (var connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        R result = operation.apply(connection);
        connection.commit();
        return result;
      } catch (SQLException e) {
        rollbackQuietly(connection);
        throw new RuntimeException("Error executing operation", e);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error getting connection", e);
    }
  }

  private Iterable<T> executeQuery(SqlConsumer<PreparedStatement> paramSetter, String query) {
    return executeInTransaction(connection -> {
      try (PreparedStatement stmt = connection.prepareStatement(query)) {
        stmt.setFetchSize(1);
        paramSetter.accept(stmt);
        ResultSet results = stmt.executeQuery();
        List<T> entities = new LinkedList<>();
        while (results.next()) {
          entities.add(parseEntityFromResultSet(results));
        }
        return entities;
      }
    });
  }

  @Override
  public Iterable<T> saveBatch(Iterable<T> entities) {
    return executeInTransaction(connection -> {
      try (PreparedStatement stmt = connection.prepareStatement(getInsertQuery())) {
        for (T entity : entities) {
          setStatementParameters(stmt, entity);
          stmt.addBatch();
        }
        stmt.executeBatch();
        return entities;
      }
    });
  }

  @Override
  public T save(T entity) {
    return executeInTransaction(connection -> {
      try (PreparedStatement stmt = connection.prepareStatement(getInsertQuery())) {
        setStatementParameters(stmt, entity);
        stmt.executeUpdate();
        return entity;
      }
    });
  }

  @Override
  public Iterable<T> findAll() {
    return executeQuery((stmt) -> {
    }, getFindAllQuery());
  }

  @Override
  public Iterable<T> findAllByTimeAndInstrumentUid(LocalDateTime time, String instrumentUid) {
    return executeQuery(stmt -> {
      stmt.setTimestamp(1, Timestamp.valueOf(time));
      stmt.setString(2, instrumentUid);
    }, getFindByTimeAndInstrumentUidQuery());
  }

  @Override
  public Iterable<T> findAllByPeriodAndInstrumentUid(
    LocalDateTime startTime,
    LocalDateTime endTime,
    String instrumentUid
  ) {
    return executeQuery(stmt -> {
      stmt.setTimestamp(1, Timestamp.valueOf(startTime));
      stmt.setTimestamp(2, Timestamp.valueOf(endTime));
      stmt.setString(3, instrumentUid);
    }, getFindByPeriodAndInstrumentUidQuery());
  }

  private void createTableIfNotExists() {
    executeInTransaction(connection -> {
      try (Statement stmt = connection.createStatement()) {
        if (schemaName != null) {
          stmt.execute(getSchemaQuery());
        }
        stmt.execute(getTableQuery());
        return null;
      }
    });
  }

  private void rollbackQuietly(Connection connection) {
    try {
      connection.rollback();
    } catch (SQLException ex) {
      throw new RuntimeException("Error during rollback", ex);
    }
  }
}
