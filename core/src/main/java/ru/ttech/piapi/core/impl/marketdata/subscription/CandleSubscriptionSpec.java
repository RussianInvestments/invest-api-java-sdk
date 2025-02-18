package ru.ttech.piapi.core.impl.marketdata.subscription;

import lombok.Getter;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;

/**
 * Свойсва подписки на свечи
 */
@Getter
public class CandleSubscriptionSpec {

  private final GetCandlesRequest.CandleSource candleSource;
  private final boolean waitingClose;

  /**
   * Свойства подписки на свечи
   * @param candleSource источник свечей
   * @param waitingClose ожидание закрытия свечи
   */
  public CandleSubscriptionSpec(GetCandlesRequest.CandleSource candleSource, boolean waitingClose) {
    this.candleSource = candleSource;
    this.waitingClose = waitingClose;
  }

  /**
   * Свойство подписки на свечи
   * <p>waitingClose = true</p>
   * @param candleSource Источник свечей
   */
  public CandleSubscriptionSpec(GetCandlesRequest.CandleSource candleSource) {
    this.candleSource = candleSource;
    this.waitingClose = true;
  }

  /**
   * Своства по умолчанию:
   * <p>waitingClose = true</p>
   * <p>candleSource = GetCandlesRequest.CandleSource.CANDLE_SOURCE_INCLUDE_WEEKEND</p>
   */
  public CandleSubscriptionSpec() {
    this.candleSource = GetCandlesRequest.CandleSource.CANDLE_SOURCE_INCLUDE_WEEKEND;
    this.waitingClose = true;
  }
}
