package ru.ttech.piapi.core.impl.marketdata;

import lombok.Getter;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;
import ru.ttech.piapi.core.impl.marketdata.subscription.SubscriptionStatus;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.LastPriceWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.OrderBookWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.TradeWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.TradingStatusWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Getter
public class MarketDataStreamContext {

  private final OnNextListener<CandleWrapper> globalOnCandleListener;
  private final OnNextListener<LastPriceWrapper> globalOnLastPriceListener;
  private final OnNextListener<TradeWrapper> globalOnTradeListener;
  private final OnNextListener<OrderBookWrapper> globalOnOrderBookListener;
  private final OnNextListener<TradingStatusWrapper> globalOnTradingStatusesListener;
  private final List<OnNextListener<CandleWrapper>> onCandleListeners = Collections.synchronizedList(new ArrayList<>());
  private final List<OnNextListener<LastPriceWrapper>> onLastPriceListeners = Collections.synchronizedList(new ArrayList<>());
  private final List<OnNextListener<TradeWrapper>> onTradeListeners = Collections.synchronizedList(new ArrayList<>());
  private final List<OnNextListener<OrderBookWrapper>> onOrderBookListeners = Collections.synchronizedList(new ArrayList<>());
  private final List<OnNextListener<TradingStatusWrapper>> onTradingStatusListeners = Collections.synchronizedList(new ArrayList<>());
  private final BlockingQueue<CandleWrapper> candleQueue = new LinkedBlockingQueue<>();
  private final BlockingQueue<LastPriceWrapper> lastPriceQueue = new LinkedBlockingQueue<>();
  private final BlockingQueue<TradeWrapper> tradesQueue = new LinkedBlockingQueue<>();
  private final BlockingQueue<OrderBookWrapper> orderBooksQueue = new LinkedBlockingQueue<>();
  private final BlockingQueue<TradingStatusWrapper> tradingStatusesQueue = new LinkedBlockingQueue<>();
  private final Map<Instrument, SubscriptionStatus> candlesSubscriptionsMap = new ConcurrentHashMap<>();
  private final Map<Instrument, SubscriptionStatus> lastPricesSubscriptionsMap = new ConcurrentHashMap<>();
  private final Map<Instrument, SubscriptionStatus> tradesSubscriptionsMap = new ConcurrentHashMap<>();
  private final Map<Instrument, SubscriptionStatus> orderBooksSubscriptionsMap = new ConcurrentHashMap<>();
  private final Map<Instrument, SubscriptionStatus> tradingStatusesSubscriptionsMap = new ConcurrentHashMap<>();

  public MarketDataStreamContext() {
    this.globalOnCandleListener = candleQueue::offer;
    this.globalOnLastPriceListener = lastPriceQueue::offer;
    this.globalOnTradeListener = tradesQueue::offer;
    this.globalOnOrderBookListener = orderBooksQueue::offer;
    this.globalOnTradingStatusesListener = tradingStatusesQueue::offer;
  }
}
