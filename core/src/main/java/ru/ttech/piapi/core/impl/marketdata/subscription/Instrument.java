package ru.ttech.piapi.core.impl.marketdata.subscription;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import ru.tinkoff.piapi.contract.v1.OrderBookType;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;

/**
 * Инструмент для подписки на рыночные данные
 * <p><b>Для подписки на разные типы данных необходимо использовать разные конструкторы</b>
 */
@Getter
@ToString
@EqualsAndHashCode
public class Instrument {

  private final String instrumentUid;
  private final SubscriptionInterval subscriptionInterval;
  private final int depth;
  private final OrderBookType orderBookType;

  /**
   * Конструктор инструмента для подписки на последнюю цену, сделки и торговый статус
   *
   * @param instrumentUid UID инструмента
   */
  public Instrument(String instrumentUid) {
    this.instrumentUid = instrumentUid;
    this.depth = 0;
    this.subscriptionInterval = null;
    this.orderBookType = null;
  }

  /**
   * Конструктор инструмента для подписки на свечи
   *
   * @param instrumentUid UID инструмента
   * @param subscriptionInterval Период одной свечи
   */
  public Instrument(String instrumentUid, SubscriptionInterval subscriptionInterval) {
    this.instrumentUid = instrumentUid;
    this.subscriptionInterval = subscriptionInterval;
    this.orderBookType = null;
    this.depth = 0;
  }

  /**
   * Конструктор инструмента для подписки на торговый стакан
   *
   * @param instrumentUid UID инструмента
   * @param depth Глубина стакана
   * @param orderBookType Тип стакана
   */
  public Instrument(String instrumentUid, int depth, OrderBookType orderBookType) {
    this.instrumentUid = instrumentUid;
    this.depth = depth;
    this.orderBookType = orderBookType;
    this.subscriptionInterval = null;
  }
}
