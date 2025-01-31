package ru.ttech.piapi.springboot.storage.jdbc.repository;

import ru.ttech.piapi.springboot.storage.core.repository.WriteRepository;
import ru.ttech.piapi.springboot.storage.jdbc.config.JdbcConfiguration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class JdbcRepository<T> implements AutoCloseable, WriteRepository<T> {
  protected final Connection connection;
  protected final String tableName;

  public JdbcRepository(JdbcConfiguration configuration) throws SQLException {
    this.connection = configuration.getDataSource().getConnection();
    this.connection.setAutoCommit(false);
    this.tableName = configuration.getTableName();
    createTableIfNotExists();
  }

  protected abstract String getTableSchema();

  protected abstract String getInsertQuery();

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
      throw new RuntimeException("Error saving entity", e);
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
      stmt.execute(getTableSchema());
      connection.commit();
    }
  }
}
