package ru.ttech.piapi.springboot.storage.core.repository;

public interface WriteRepository<T> {

  Iterable<T> saveBatch(Iterable<T> entities);

  T save(T entity);
}
