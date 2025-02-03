package ru.ttech.piapi.springboot.storage.jdbc.repository;

import java.sql.SQLException;

@FunctionalInterface
public interface SqlFunction<T, R> {

  R apply(T t) throws SQLException;
}
