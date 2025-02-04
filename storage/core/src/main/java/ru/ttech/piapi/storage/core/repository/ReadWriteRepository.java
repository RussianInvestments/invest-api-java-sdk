package ru.ttech.piapi.storage.core.repository;

import ru.tinkoff.piapi.contract.v1.Candle;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.OrderBook;
import ru.tinkoff.piapi.contract.v1.Trade;
import ru.tinkoff.piapi.contract.v1.TradingStatus;

import java.time.LocalDateTime;

/**
 * Общий интерфейс репозиториев для хранения рыночных данных
 *
 * @param <T> тип данных, хранимых в репозитории.
 *            <p>На данный момент поддерживаются следующие типы данных:
 *            <ul>
 *              <li>{@link Candle}</li>
 *              <li>{@link LastPrice}</li>
 *              <li>{@link Trade}</li>
 *              <li>{@link TradingStatus}</li>
 *              <li>{@link OrderBook}</li>
 *            </ul>
 */
public interface ReadWriteRepository<T> {

  /**
   * Метод для сохранения списка сущностей в репозиторий
   *
   * @param entities Список сущностей
   * @return Возвращает тот же список, который передан в параметрах
   */
  Iterable<T> saveBatch(Iterable<T> entities);

  /**
   * Метод для сохранения сущности в репозиторий
   *
   * @param entity Сущность
   * @return Возвращает ту же сущность, которая передана в параметрах
   */
  T save(T entity);

  /**
   * Метод для получения всех сущностей из репозитория
   *
   * @return Возвращает список всех сущностей
   */
  Iterable<T> findAll();

  /**
   * Метод для поиска всех сущностей с указанным временем и instrumentUid
   *
   * @param time          Время
   * @param instrumentUid Инструмент
   * @return Список сущностей, найденных в репозитории по переданным параметрам
   */
  Iterable<T> findAllByTimeAndInstrumentUid(LocalDateTime time, String instrumentUid);

  /**
   * Метод для поиска всех сущностей за определённый период времени с указанным instrumentUid,
   * начиная со startTime (включительно) и заканчивая endTime (включительно)
   *
   * @param startTime     Начало периода
   * @param endTime       Конец периода
   * @param instrumentUid Инструмент
   * @return Список сущностей, найденных в репозитории по переданным параметрам
   */
  Iterable<T> findAllByPeriodAndInstrumentUid(
    LocalDateTime startTime, LocalDateTime endTime, String instrumentUid
  );
}
