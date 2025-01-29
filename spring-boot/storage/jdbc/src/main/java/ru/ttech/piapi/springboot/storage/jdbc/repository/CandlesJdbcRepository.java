package ru.ttech.piapi.springboot.storage.jdbc.repository;

import ru.tinkoff.piapi.contract.v1.Candle;
import ru.ttech.piapi.springboot.storage.repository.CandlesRepository;

public class CandlesJdbcRepository implements CandlesRepository {

  @Override
  public Iterable<Candle> saveBatch(Iterable<Candle> entities) {
    // TODO: сохранить батч в БД
    return entities;
  }

  @Override
  public Candle save(Candle entity) {
    // TODO: сохранить сущность в БД
    return entity;
  }
}
