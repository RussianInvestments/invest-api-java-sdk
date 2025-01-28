package ru.ttech.piapi.core.impl.marketdata.wrapper;

import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.LastPriceType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.core.impl.wrapper.ResponseWrapper;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Информация о цене последней сделки
 * <p>Обертка для работы с {@link LastPrice}
 */
public class LastPriceWrapper extends ResponseWrapper<LastPrice> {

  public LastPriceWrapper(LastPrice lastPrice) {
    super(lastPrice);
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
   * Метод для получения цены последней сделки за 1 инструмент
   * <p>Чтобы получить стоимость лота, нужно умножить на лотность инструмента.
   * <a href="https://developer.tbank.ru/invest/intro/developer/table_order_currency">
   * Подробнее про перевод цен в валюту
   * </a>
   * @return Цена последней сделки
   */
  public BigDecimal getPrice() {
    return NumberMapper.quotationToBigDecimal(response.getPrice());
  }

  /**
   * Метод для получения времени последней цены в часовом поясе UTC по времени биржи
   *
   * @return Время последней цены
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
   * Метод для получения типа последней цены
   *
   * @return Тип последней цены
   */
  public LastPriceType getLastPriceType() {
    return response.getLastPriceType();
  }
}
