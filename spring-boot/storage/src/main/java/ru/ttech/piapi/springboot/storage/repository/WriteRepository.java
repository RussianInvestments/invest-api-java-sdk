package ru.ttech.piapi.springboot.storage.repository;

public interface WriteRepository<T> {

  Iterable<T> saveBatch(Iterable<T> entities);

  T save(T entity);
}
