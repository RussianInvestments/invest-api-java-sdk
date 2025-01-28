package ru.ttech.piapi.core.impl.marketdata.wrapper;

import ru.tinkoff.piapi.contract.v1.OrderBook;
import ru.ttech.piapi.core.impl.wrapper.ResponseWrapper;

public class OrderBookWrapper extends ResponseWrapper<OrderBook> {

  public OrderBookWrapper(OrderBook orderBook) {
    super(orderBook);
  }

}
