package ru.ttech.piapi.core.impl.marketdata.wrapper;

import ru.tinkoff.piapi.contract.v1.Order;
import ru.tinkoff.piapi.contract.v1.OrderBook;
import ru.tinkoff.piapi.contract.v1.OrderBookType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.core.impl.wrapper.ResponseWrapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Стакан
 * <p>Обертка для работы с {@link OrderBook}
 */
public class OrderBookWrapper extends ResponseWrapper<OrderBook> {

  private final List<OrderWrapper> bids;
  private final List<OrderWrapper> asks;

  public OrderBookWrapper(OrderBook orderBook) {
    super(orderBook);
    this.bids = orderBook.getBidsList().stream().map(OrderWrapper::new)
      .collect(Collectors.toUnmodifiableList());
    this.asks = orderBook.getAsksList().stream().map(OrderWrapper::new)
      .collect(Collectors.toUnmodifiableList());
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
   * Метод для получния глубины стакана
   *
   * @return Глубина стакана
   */
  public int getDepth() {
    return response.getDepth();
  }

  /**
   * Метод для получения флага консистентности стакана
   *
   * @return Флаг консистентности стакана
   */
  public boolean isConsistent() {
    return response.getIsConsistent();
  }

  /**
   * Метод для получения списка предложений
   *
   * @return список предложений
   */
  public List<OrderWrapper> getBids() {
    return bids;
  }

  /**
   * Метод для получения списка спроса
   *
   * @return список спроса
   */
  public List<OrderWrapper> getAsks() {
    return asks;
  }

  /**
   * Метод для получения времени формирования стакана в часовом поясе UTC по времени биржи.
   *
   * @return Время формирования стакана
   */
  public LocalDate getTime() {
    return TimeMapper.timestampToLocalDate(response.getTime());
  }

  /**
   * Метод для полуения верхнего лимита цены за 1 инструмент
   * <p>Чтобы получить стоимость лота, нужно умножить на лотность инструмента.
   * <a href="https://developer.tbank.ru/invest/intro/developer/table_order_currency">
   * Подробнее про перевод цен в валюту
   * </a>
   *
   * @return Верхний лимит цены
   */
  public BigDecimal getLimitUp() {
    return NumberMapper.quotationToBigDecimal(response.getLimitUp());
  }

  /**
   * Метод для полуения нижнего лимита цены за 1 инструмент
   * <p>Чтобы получить стоимость лота, нужно умножить на лотность инструмента.
   * <a href="https://developer.tbank.ru/invest/intro/developer/table_order_currency">
   * Подробнее про перевод цен в валюту
   * </a>
   *
   * @return Нижний лимит цены
   */
  public BigDecimal getLimitDown() {
    return NumberMapper.quotationToBigDecimal(response.getLimitDown());
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
   * Метод для получения типа стакана
   *
   * @return Тип стакана
   */
  public OrderBookType getOrderBookType() {
    return response.getOrderBookType();
  }

  /**
   * Ордер на продажу/покупку
   * <p>Обертка для работы с {@link Order}
   */
  public static class OrderWrapper extends ResponseWrapper<Order> {

    protected OrderWrapper(Order response) {
      super(response);
    }

    /**
     * Метод для получения цены за 1 инструмент
     * <p>Чтобы получить стоимость лота, нужно умножить на лотность инструмента.
     * <a href="https://developer.tbank.ru/invest/intro/developer/table_order_currency">
     * Подробнее про перевод цен в валюту
     * </a>
     *
     * @return Верхний лимит цены
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
  }
}
