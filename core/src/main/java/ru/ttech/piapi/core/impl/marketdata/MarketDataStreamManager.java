package ru.ttech.piapi.core.impl.marketdata;

import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.contract.v1.MarketDataStreamServiceGrpc;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.streaming.BidirectionalStreamWrapper;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.listeners.OnCompleteListener;
import ru.ttech.piapi.core.connector.streaming.listeners.OnErrorListener;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.LastPriceWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.OrderBookWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.TradeWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.TradingStatusWrapper;

import java.util.ArrayList;
import java.util.List;

public class MarketDataStreamManager {

  private final StreamServiceStubFactory streamFactory;
  private final ConnectorConfiguration configuration;
  private final OnNextListener<CandleWrapper> globalOnCandleListener;
  private final OnNextListener<LastPriceWrapper> globalOnLastPriceListener;
  private final OnNextListener<OrderBookWrapper> globalOnOrderBookListener;
  private final OnNextListener<TradeWrapper> globalOnTradeListener;
  private final OnNextListener<TradingStatusWrapper> globalOnTradingStatusListener;
  private final OnErrorListener globalOnErrorListener;
  private final OnCompleteListener globalOnCompleteListener;
  private final List<OnNextListener<CandleWrapper>> onCandleListeners = new ArrayList<>();
  private final List<OnNextListener<LastPriceWrapper>> onLastPriceListeners = new ArrayList<>();
  private final List<OnNextListener<OrderBookWrapper>> onOrderBookListeners = new ArrayList<>();
  private final List<OnNextListener<TradeWrapper>> onTradeListeners = new ArrayList<>();
  private final List<OnNextListener<TradingStatusWrapper>> onTradingStatusListeners = new ArrayList<>();
  private final List<OnErrorListener> onErrorListeners = new ArrayList<>();
  private final List<OnCompleteListener> onCompleteListeners = new ArrayList<>();
  private final List<StreamTuple> streamWrappers = new ArrayList<>();

  public MarketDataStreamManager(StreamServiceStubFactory streamFactory) {
    this.streamFactory = streamFactory;
    this.configuration = streamFactory.getServiceStubFactory().getConfiguration();
    // TODO: нужна ли синхронизация?
    this.globalOnCandleListener = candle -> onCandleListeners.forEach(listener -> listener.onNext(candle));
    this.globalOnLastPriceListener = lastPrice -> onLastPriceListeners.forEach(listener -> listener.onNext(lastPrice));
    this.globalOnOrderBookListener = orderBook -> onOrderBookListeners.forEach(listener -> listener.onNext(orderBook));
    this.globalOnTradeListener = trade -> onTradeListeners.forEach(listener -> listener.onNext(trade));
    this.globalOnTradingStatusListener = tradingStatus -> onTradingStatusListeners.forEach(listener -> listener.onNext(tradingStatus));
    this.globalOnErrorListener = throwable -> onErrorListeners.forEach(listener -> listener.onError(throwable));
    this.globalOnCompleteListener = () -> onCompleteListeners.forEach(OnCompleteListener::onComplete);
  }

  /**
   * Подписывает на получение рыночных данных
   * Внимание! Подразумевается, что этот метод принимает только запрос на подписку
   *
   * @param request
   */
  public void subscribe(MarketDataRequest request) {
    streamWrappers.stream()
      .filter(tuple -> tuple.getSubscriptionsCount() < configuration.getMaxMarketDataSubscriptionsCount())
      .findFirst()
      .ifPresentOrElse(wrapper -> {
          wrapper.getStream().newCall(request);
          wrapper.incrementSubscriptionsCount();
        },
        () -> {
          // TODO: тут нужно делать запрос к апи и смотреть, сколько стримов осталось
          if (streamWrappers.size() >= configuration.getMaxMarketDataStreamsCount()) {
            throw new IllegalStateException("Maximum number of streams exceeded");
          }
          var newWrapper = streamFactory.newBidirectionalStream(
            MarketDataStreamConfiguration.builder()
              .addOnCandleListener(globalOnCandleListener)
              .addOnLastPriceListener(globalOnLastPriceListener)
              .addOnOrderBookListener(globalOnOrderBookListener)
              .addOnTradeListener(globalOnTradeListener)
              .addOnTradingStatusListener(globalOnTradingStatusListener)
              .addOnErrorListener(globalOnErrorListener)
              .addOnCompleteListener(globalOnCompleteListener)
              .build());
          newWrapper.connect();
          newWrapper.newCall(request);
          var tuple = new StreamTuple(newWrapper);
          tuple.incrementSubscriptionsCount();
          streamWrappers.add(tuple);
        });
  }

  public void addOnCandleListener(OnNextListener<CandleWrapper> onCandleListener) {
    onCandleListeners.add(onCandleListener);
  }

  public void addOnLastPriceListener(OnNextListener<LastPriceWrapper> onLastPriceListener) {
    onLastPriceListeners.add(onLastPriceListener);
  }

  public void addOnOrderBookListener(OnNextListener<OrderBookWrapper> onOrderBookListener) {
    onOrderBookListeners.add(onOrderBookListener);
  }

  public void addOnTradeListener(OnNextListener<TradeWrapper> onTradeListener) {
    onTradeListeners.add(onTradeListener);
  }

  public void addOnTradingStatusListener(OnNextListener<TradingStatusWrapper> onTradingStatusListener) {
    onTradingStatusListeners.add(onTradingStatusListener);
  }

  public void addOnErrorListener(OnErrorListener onErrorListener) {
    onErrorListeners.add(onErrorListener);
  }

  public void addOnCompleteListener(OnCompleteListener onCompleteListener) {
    onCompleteListeners.add(onCompleteListener);
  }

  public StreamServiceStubFactory getStreamFactory() {
    return streamFactory;
  }

  public void shutdown() {
    streamWrappers.forEach(tuple -> tuple.getStream().disconnect());
  }

  public static class StreamTuple {

    private final BidirectionalStreamWrapper<
      MarketDataStreamServiceGrpc.MarketDataStreamServiceStub,
      MarketDataRequest,
      MarketDataResponse> stream;
    private int subscriptionsCount;

    public StreamTuple(
      BidirectionalStreamWrapper<MarketDataStreamServiceGrpc.MarketDataStreamServiceStub,
        MarketDataRequest,
        MarketDataResponse> stream) {
      this.stream = stream;
      this.subscriptionsCount = 1;
    }

    public BidirectionalStreamWrapper<
      MarketDataStreamServiceGrpc.MarketDataStreamServiceStub,
      MarketDataRequest,
      MarketDataResponse> getStream() {
      return stream;
    }

    public int getSubscriptionsCount() {
      return subscriptionsCount;
    }

    public void incrementSubscriptionsCount() {
      subscriptionsCount++;
    }
  }
}
