package ru.ttech.piapi.core.impl.marketdata.wrapper;

import ru.tinkoff.piapi.contract.v1.Trade;
import ru.tinkoff.piapi.contract.v1.TradeDirection;
import ru.tinkoff.piapi.contract.v1.TradeSourceType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.core.impl.wrapper.ResponseWrapper;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Информация о сделке
 * <p>Обертка для работы с {@link Trade}
 */
public class TradeWrapper extends ResponseWrapper<Trade> {

  public TradeWrapper(Trade trade) {
    super(trade);
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
   * Метод для получения направления сделки
   *
   * @return Направление сделки
   */
  public TradeDirection getDirection() {
    return response.getDirection();
  }

  /**
   * Метод для получения цены за 1 инструмент
   *
   * @return Цена
   */
  public BigDecimal getPrice() {
    return NumberMapper.quotationToBigDecimal(response.getPrice());
  }

  /**
   * Метод для получения количества лотов
   *
   * @return Количество лотов
   */
  public long getQuantity() {
    return response.getQuantity();
  }

  /**
   * Метод для получения времени сделки в часовом поясе UTC по времени биржи
   *
   * @return Время сделки
   */
  public LocalDate getTime() {
    return TimeMapper.timestampToLocalDate(response.getTime());
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
   * Метод для получения типа источника сделки
   *
   * @return тип источника сделки
   */
  public TradeSourceType getTradeSource() {
    return response.getTradeSource();
  }
}
