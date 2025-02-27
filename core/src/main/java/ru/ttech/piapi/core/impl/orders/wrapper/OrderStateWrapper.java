package ru.ttech.piapi.core.impl.orders.wrapper;

import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderExecutionReportStatus;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamResponse;
import ru.tinkoff.piapi.contract.v1.OrderTrade;
import ru.tinkoff.piapi.contract.v1.OrderType;
import ru.tinkoff.piapi.contract.v1.TimeInForceType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.core.impl.wrapper.ResponseWrapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Заявка
 * <p>Обертка для работы с {@link OrderStateStreamResponse.OrderState}
 */
public class OrderStateWrapper extends ResponseWrapper<OrderStateStreamResponse.OrderState> {

  public OrderStateWrapper(OrderStateStreamResponse.OrderState orderState) {
    super(orderState);
  }

  /**
   * @return биржевой идентификатор заявки.
   */
  public String getOrderId() {
    return response.getOrderId();
  }

  /**
   * @return Идентификатор ключа идемпотентности, переданный клиентом, в формате UID
   */
  public Optional<String> getOrderRequestId() {
    return response.hasOrderRequestId()
      ? Optional.of(response.getOrderRequestId())
      : Optional.empty();
  }

  /**
   * @return Код клиента на бирже.
   */
  public String getClientCode() {
    return response.getClientCode();
  }

  /**
   * @return Дата создания заявки.
   */
  public LocalDateTime getCreatedAt() {
    return TimeMapper.timestampToLocalDateTime(response.getCreatedAt());
  }

  /**
   * @return Статус заявки.
   */
  public OrderExecutionReportStatus getExecutionReportStatus() {
    return response.getExecutionReportStatus();
  }

  /**
   * @return Дополнительная информация по статусу.
   */
  public Optional<OrderStateStreamResponse.StatusCauseInfo> getStatusInfo() {
    return response.hasStatusInfo() ?
      Optional.of(response.getStatusInfo())
      : Optional.empty();
  }

  /**
   * @return Тикер инструмента.
   */
  public String getTicker() {
    return response.getTicker();
  }

  /**
   * @return Класс-код (секция торгов).
   */
  public String getClassCode() {
    return response.getClassCode();
  }

  /**
   * @return Лотность инструмента заявки.
   */
  public int getLotSize() {
    return response.getLotSize();
  }

  /**
   * @return Направление заявки.
   */
  public OrderDirection getDirection() {
    return response.getDirection();
  }

  /**
   * @return Алгоритм исполнения поручения.
   */
  public TimeInForceType getTimeInForce() {
    return response.getTimeInForce();
  }

  /**
   * @return Тип заявки.
   */
  public OrderType getOrderType() {
    return response.getOrderType();
  }

  /**
   * @return Номер счета.
   */
  public String getAccountId() {
    return response.getAccountId();
  }

  /**
   * @return Начальная цена заявки.
   */
  public BigDecimal getInitialOrderPrice() {
    return NumberMapper.moneyValueToBigDecimal(response.getInitialOrderPrice());
  }

  /**
   * @return Цена выставления заявки.
   */
  public BigDecimal getOrderPrice() {
    return NumberMapper.moneyValueToBigDecimal(response.getOrderPrice());
  }

  /**
   * @return Предрассчитанная стоимость полной заявки.
   */
  public Optional<BigDecimal> getAmount() {
    return response.hasAmount()
      ? Optional.of(NumberMapper.moneyValueToBigDecimal(response.getAmount()))
      : Optional.empty();
  }

  /**
   * @return Исполненная цена заявки.
   */
  public BigDecimal getExecutedOrderPrice() {
    return NumberMapper.moneyValueToBigDecimal(response.getExecutedOrderPrice());
  }

  /**
   * @return Валюта исполнения.
   */
  public String getCurrency() {
    return response.getCurrency();
  }

  /**
   * @return Запрошено лотов.
   */
  public long getLotsRequested() {
    return response.getLotsRequested();
  }

  /**
   * @return Исполнено лотов.
   */
  public long getLotsExecuted() {
    return response.getLotsExecuted();
  }

  /**
   * @return Число неисполненных лотов по заявке.
   */
  public long getLotsLeft() {
    return response.getLotsLeft();
  }

  /**
   * @return Отмененные лоты.
   */
  public long getLotsCancelled() {
    return response.getLotsCancelled();
  }

  /**
   * @return Спецсимвол.
   */
  public Optional<OrderStateStreamResponse.MarkerType> getMarker() {
    return response.hasMarker()
      ? Optional.of(response.getMarker())
      : Optional.empty();
  }

  /**
   * @return Список сделок.
   */
  public List<OrderTrade> getTrades() {
    return response.getTradesList();
  }

  /**
   * @return Время исполнения заявки.
   */
  public LocalDateTime getCompletionTime() {
    return TimeMapper.timestampToLocalDateTime(response.getCompletionTime());
  }

  /**
   * @return Код биржи.
   */
  public String getExchange() {
    return response.getExchange();
  }

  /**
   * @return UID идентификатор инструмента.
   */
  public String getInstrumentUid() {
    return response.getInstrumentUid();
  }
}

