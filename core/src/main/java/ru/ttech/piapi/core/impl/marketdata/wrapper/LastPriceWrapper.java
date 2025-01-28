package ru.ttech.piapi.core.impl.marketdata.wrapper;

import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.ttech.piapi.core.impl.wrapper.ResponseWrapper;

public class LastPriceWrapper extends ResponseWrapper<LastPrice> {

  public LastPriceWrapper(LastPrice lastPrice) {
    super(lastPrice);
  }
}
