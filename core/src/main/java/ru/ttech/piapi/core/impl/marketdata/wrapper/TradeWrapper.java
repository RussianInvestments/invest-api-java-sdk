package ru.ttech.piapi.core.impl.marketdata.wrapper;

import ru.tinkoff.piapi.contract.v1.Trade;
import ru.ttech.piapi.core.impl.wrapper.ResponseWrapper;

public class TradeWrapper extends ResponseWrapper<Trade> {

  public TradeWrapper(Trade trade) {
    super(trade);
  }
}
