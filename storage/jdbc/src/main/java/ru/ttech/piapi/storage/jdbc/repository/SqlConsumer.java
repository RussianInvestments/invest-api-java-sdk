package ru.ttech.piapi.storage.jdbc.repository;

import java.sql.SQLException;

@FunctionalInterface
public interface SqlConsumer<T> {

  void accept(T t) throws SQLException;
}
