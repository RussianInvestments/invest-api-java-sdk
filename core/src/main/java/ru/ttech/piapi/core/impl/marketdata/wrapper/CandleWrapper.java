package ru.ttech.piapi.core.impl.marketdata.wrapper;

import ru.tinkoff.piapi.contract.v1.Candle;
import ru.tinkoff.piapi.contract.v1.CandleSource;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.core.impl.wrapper.ResponseWrapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Свеча
 * <p>Обертка для работы с {@link Candle}
 */
public class CandleWrapper extends ResponseWrapper<Candle> {

  public CandleWrapper(Candle candle) {
    super(candle);
  }

  /**
   * Метод для получения FIGI-идентификатора инструмента
   *
   * @return FIGI-идентификатор инструмента
   */
  public String getFigi() {
    return response.getFigi();
  }

  /**
   * Метод для получения интервала свечи
   *
   * @return Интервал свечи
   */
  public SubscriptionInterval getInterval() {
    return response.getInterval();
  }

  /**
   * Метод для полуения цены открытия за 1 инструмент
   * <p>Чтобы получить стоимость лота, нужно умножить на лотность инструмента.
   * <a href="https://developer.tbank.ru/invest/intro/developer/table_order_currency">
   * Подробнее про перевод цен в валюту
   * </a>
   *
   * @return Цена открытия
   */
  public BigDecimal getOpen() {
    return NumberMapper.quotationToBigDecimal(response.getOpen());
  }

  /**
   * Метод для полуения максимальной цены за 1 инструмент
   * <p>Чтобы получить стоимость лота, нужно умножить на лотность инструмента.
   * <a href="https://developer.tbank.ru/invest/intro/developer/table_order_currency">
   * Подробнее про перевод цен в валюту
   * </a>
   *
   * @return Максимальная цена
   */
  public BigDecimal getHigh() {
    return NumberMapper.quotationToBigDecimal(response.getHigh());
  }

  /**
   * Метод для полуения минимальной цены за 1 инструмент
   * <p>Чтобы получить стоимость лота, нужно умножить на лотность инструмента.
   * <a href="https://developer.tbank.ru/invest/intro/developer/table_order_currency">
   * Подробнее про перевод цен в валюту
   * </a>
   *
   * @return Минимальная цена
   */
  public BigDecimal getLow() {
    return NumberMapper.quotationToBigDecimal(response.getLow());
  }

  /**
   * Метод для полуения цены закрытия за 1 инструмент
   * <p>Чтобы получить стоимость лота, нужно умножить на лотность инструмента.
   * <a href="https://developer.tbank.ru/invest/intro/developer/table_order_currency">
   * Подробнее про перевод цен в валюту
   * </a>
   *
   * @return Цена закрытия
   */
  public BigDecimal getClose() {
    return NumberMapper.quotationToBigDecimal(response.getClose());
  }

  /**
   * Метод для получения объёма сделок в лотах
   *
   * @return Объём сделок в лотах
   */
  public long getVolume() {
    return response.getVolume();
  }

  /**
   * Мето для получения времени начала интервала свечи по UTC
   *
   * @return Время начала интервала
   */
  public LocalDateTime getTime() {
    return TimeMapper.timestampToLocalDateTime(response.getTime());
  }

  /**
   * Метод для получения времени последней сделки, вошедшей в свечу по UTC
   *
   * @return Время последней сделки
   */
  public LocalDateTime getLastTradeTime() {
    return TimeMapper.timestampToLocalDateTime(response.getLastTradeTs());
  }

  /**
   * Метод для получения UID инструмента
   *
   * @return UID инструмента
   */
  public String getInstrumentUid() {
    return response.getInstrumentUid();
  }

  /**
   * Метод для получения типа источника свечей
   *
   * @return Тип источника свечей
   */
  public CandleSource getCandleSourceType() {
    return response.getCandleSourceType();
  }
}
