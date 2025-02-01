package ru.ttech.piapi.springboot.storage.jdbc.repository;

import ru.ttech.piapi.springboot.storage.core.repository.ReadWriteRepository;
import ru.ttech.piapi.springboot.storage.jdbc.config.JdbcConfiguration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

public abstract class JdbcRepository<T> implements AutoCloseable, ReadWriteRepository<T> {

  protected final Connection connection;
  protected final String tableName;
  protected final String schemaName;

  public JdbcRepository(JdbcConfiguration configuration) throws SQLException {
    this.connection = configuration.getDataSource().getConnection();
    this.connection.setAutoCommit(false);
    this.schemaName = configuration.getSchemaName();
    this.tableName = configuration.getTableName();
    createTableIfNotExists();
  }

  protected final String getTableName() {
    return String.format("%s.%s", schemaName, tableName);
  }

  protected String getSchemaQuery() {
    return "CREATE SCHEMA IF NOT EXISTS " + schemaName;
  }

  protected abstract String getTableQuery();

  protected abstract String getInsertQuery();

  protected String getFindAllQuery() {
    return "SELECT * FROM " + getTableName();
  }

  protected String getFindByTimeAndInstrumentUidQuery() {
    return "SELECT * FROM " + getTableName() + " WHERE time =? AND instrument_uid =?";
  }

  protected abstract T parseEntityFromResultSet(ResultSet rs) throws SQLException;

  protected abstract void setStatementParameters(PreparedStatement stmt, T entity) throws SQLException;

  @Override
  public Iterable<T> saveBatch(Iterable<T> entities) {
    try (PreparedStatement stmt = connection.prepareStatement(getInsertQuery())) {
      for (T entity : entities) {
        setStatementParameters(stmt, entity);
        stmt.addBatch();
      }
      stmt.executeBatch();
      connection.commit();
      return entities;
    } catch (SQLException e) {
      try {
        connection.rollback();
      } catch (SQLException ex) {
        throw new RuntimeException("Error during rollback", ex);
      }
      throw new RuntimeException("Error saving batch", e);
    }
  }

  @Override
  public T save(T entity) {
    try (PreparedStatement stmt = connection.prepareStatement(getInsertQuery())) {
      setStatementParameters(stmt, entity);
      stmt.executeUpdate();
      connection.commit();
      return entity;
    } catch (SQLException e) {
      try {
        connection.rollback();
      } catch (SQLException ex) {
        throw new RuntimeException("Error during rollback", ex);
      }
      throw new RuntimeException("Error saving entity", e);
    }
  }

  @Override
  public Iterable<T> findAll() {
    try (PreparedStatement stmt = connection.prepareStatement(getFindAllQuery())) {
      stmt.setFetchSize(1);
      ResultSet results = stmt.executeQuery();
      List<T> entities = new LinkedList<>();
      while (results.next()) {
        entities.add(parseEntityFromResultSet(results));
      }
      return entities;
    } catch (SQLException e) {
      throw new RuntimeException("Error finding entities", e);
    }
  }

  @Override
  public Iterable<T> findAllByTimeAndInstrumentUid(LocalDateTime time, String instrumentUid) {
    try (PreparedStatement stmt = connection.prepareStatement(getFindByTimeAndInstrumentUidQuery())) {
      stmt.setFetchSize(1);
      stmt.setTimestamp(1, Timestamp.valueOf(time));
      stmt.setString(2, instrumentUid);
      ResultSet results = stmt.executeQuery();
      List<T> entities = new LinkedList<>();
      while (results.next()) {
        entities.add(parseEntityFromResultSet(results));
      }
      return entities;
    } catch (SQLException e) {
      throw new RuntimeException("Error finding entities", e);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
      }
    } catch (SQLException e) {
      throw new IOException("Error closing connection", e);
    }
  }

  private void createTableIfNotExists() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.execute(getSchemaQuery());
      stmt.execute(getTableQuery());
      connection.commit();
    }
  }
}
