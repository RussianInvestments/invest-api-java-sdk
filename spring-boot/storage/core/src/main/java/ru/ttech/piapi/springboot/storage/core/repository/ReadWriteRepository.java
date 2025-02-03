package ru.ttech.piapi.springboot.storage.core.repository;

import java.time.LocalDateTime;

public interface ReadWriteRepository<T> {

  Iterable<T> saveBatch(Iterable<T> entities);

  T save(T entity);

  Iterable<T> findAll();

  Iterable<T> findAllByTimeAndInstrumentUid(LocalDateTime time, String instrumentUid);

  Iterable<T> findAllByPeriodAndInstrumentUid(
    LocalDateTime startTime, LocalDateTime endTime, String instrumentUid
  );
}
