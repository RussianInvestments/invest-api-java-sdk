package ru.ttech.piapi.core.impl.marketdata.wrapper;

import ru.tinkoff.piapi.contract.v1.SecurityTradingStatus;
import ru.tinkoff.piapi.contract.v1.TradingStatus;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.core.impl.wrapper.ResponseWrapper;

import java.time.LocalDate;

/**
 * Информация об изменении торгового статуса
 * <p>Обертка для работы с {@link TradingStatus}
 */
public class TradingStatusWrapper extends ResponseWrapper<TradingStatus> {

  public TradingStatusWrapper(TradingStatus tradingStatus) {
    super(tradingStatus);
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
   * Метод для получения обновлённого торгового статуса
   *
   * @return Статус торговли инструментом
   */
  public SecurityTradingStatus getTradingStatus() {
    return response.getTradingStatus();
  }

  /**
   * Метод для получения времени изменения торгового статуса по UTC
   *
   * @return Время изменения торгового статуса
   */
  public LocalDate getTime() {
    return TimeMapper.timestampToLocalDate(response.getTime());
  }

  /**
   * Метод для получения признака доступности выставления лимитной заявки по инструменту
   *
   * @return Доступность лимитной заявки
   */
  public boolean isLimitOrderAvailable() {
    return response.getLimitOrderAvailableFlag();
  }

  /**
   * Метод для получения признака доступности выставления рыночной заявки по инструменту.
   *
   * @return Доступность рыночной заявки
   */
  public boolean isMarketOrderAvailable() {
    return response.getMarketOrderAvailableFlag();
  }

  /**
   * Метод для получения UID инструмента
   *
   * @return UID инструмента
   */
  public String getInstrumentUid() {
    return response.getInstrumentUid();
  }

}
