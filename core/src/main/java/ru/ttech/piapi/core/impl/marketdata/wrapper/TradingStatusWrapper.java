package ru.ttech.piapi.core.impl.marketdata.wrapper;

import ru.tinkoff.piapi.contract.v1.TradingStatus;
import ru.ttech.piapi.core.impl.wrapper.ResponseWrapper;

public class TradingStatusWrapper extends ResponseWrapper<TradingStatus> {

  public TradingStatusWrapper(TradingStatus tradingStatus) {
    super(tradingStatus);
  }
}
