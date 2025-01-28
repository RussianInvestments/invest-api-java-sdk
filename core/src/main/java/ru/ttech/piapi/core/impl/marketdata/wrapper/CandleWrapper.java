package ru.ttech.piapi.core.impl.marketdata.wrapper;

import ru.tinkoff.piapi.contract.v1.Candle;
import ru.ttech.piapi.core.impl.wrapper.ResponseWrapper;

public class CandleWrapper extends ResponseWrapper<Candle> {

  public CandleWrapper(Candle candle) {
    super(candle);
  }
}
