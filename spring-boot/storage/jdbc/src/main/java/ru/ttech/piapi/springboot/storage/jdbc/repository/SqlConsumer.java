package ru.ttech.piapi.springboot.storage.jdbc.repository;

import java.sql.SQLException;

@FunctionalInterface
public interface SqlConsumer<T> {

  void accept(T t) throws SQLException;
}
