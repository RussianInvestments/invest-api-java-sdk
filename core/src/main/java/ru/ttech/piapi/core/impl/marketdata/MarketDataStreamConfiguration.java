package ru.ttech.piapi.core.impl.marketdata;

import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.StreamObserver;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.contract.v1.MarketDataStreamServiceGrpc;
import ru.ttech.piapi.core.connector.streaming.BidirectionalStreamConfiguration;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.LastPriceWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.OrderBookWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.TradeWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.TradingStatusWrapper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MarketDataStreamConfiguration
  extends BidirectionalStreamConfiguration<MarketDataStreamServiceGrpc.MarketDataStreamServiceStub,
  MarketDataRequest, MarketDataResponse> {

  private MarketDataStreamConfiguration(
    Function<Channel, MarketDataStreamServiceGrpc.MarketDataStreamServiceStub> stubConstructor,
    MethodDescriptor<MarketDataRequest, MarketDataResponse> method,
    BiFunction<MarketDataStreamServiceGrpc.MarketDataStreamServiceStub,
      StreamObserver<MarketDataResponse>,
      StreamObserver<MarketDataRequest>> call,
    MarketDataStreamObserver observer
  ) {
    super(stubConstructor, method, call, observer);
  }

  public static Builder builder() {
    return new Builder(
      MarketDataStreamServiceGrpc::newStub,
      MarketDataStreamServiceGrpc.getMarketDataStreamMethod(),
      MarketDataStreamServiceGrpc.MarketDataStreamServiceStub::marketDataStream
    );
  }

  public static class Builder
    extends BidirectionalStreamConfiguration.Builder<MarketDataStreamServiceGrpc.MarketDataStreamServiceStub,
    MarketDataRequest, MarketDataResponse> {

    private final Map<MarketDataResponseType,
      List<OnNextListener<MarketDataResponse>>> onResponseListeners = new EnumMap<>(MarketDataResponseType.class);

    protected Builder(
      Function<Channel, MarketDataStreamServiceGrpc.MarketDataStreamServiceStub> stubConstructor,
      MethodDescriptor<MarketDataRequest, MarketDataResponse> method,
      BiFunction<MarketDataStreamServiceGrpc.MarketDataStreamServiceStub,
        StreamObserver<MarketDataResponse>,
        StreamObserver<MarketDataRequest>> call) {
      super(stubConstructor, method, call);
    }

    public Builder addOnCandleListener(OnNextListener<CandleWrapper> onCandleListener) {
      return addMarketDataResponseListener(
        MarketDataResponseType.CANDLE,
        response -> onCandleListener.onNext(new CandleWrapper(response.getCandle()))
      );
    }

    public Builder addOnLastPriceListener(OnNextListener<LastPriceWrapper> onLastPriceListener) {
      return addMarketDataResponseListener(
        MarketDataResponseType.LAST_PRICE,
        response -> onLastPriceListener.onNext(new LastPriceWrapper(response.getLastPrice()))
      );
    }

    public Builder addOnOrderBookListener(OnNextListener<OrderBookWrapper> onOrderBookListener) {
      return addMarketDataResponseListener(
        MarketDataResponseType.ORDER_BOOK,
        response -> onOrderBookListener.onNext(new OrderBookWrapper(response.getOrderbook()))
      );
    }

    public Builder addOnTradeListener(OnNextListener<TradeWrapper> onTradeListener) {
      return addMarketDataResponseListener(
        MarketDataResponseType.TRADE,
        response -> onTradeListener.onNext(new TradeWrapper(response.getTrade()))
      );
    }

    public Builder addOnTradingStatusListener(OnNextListener<TradingStatusWrapper> onTradingStatusListener) {
      return addMarketDataResponseListener(
        MarketDataResponseType.TRADING_STATUS,
        response -> onTradingStatusListener.onNext(new TradingStatusWrapper(response.getTradingStatus()))
      );
    }

    private Builder addMarketDataResponseListener(
      MarketDataResponseType responseType,
      OnNextListener<MarketDataResponse> onNextListener
    ) {
      onResponseListeners.computeIfAbsent(responseType, __ -> new ArrayList<>()).add(onNextListener);
      return this;
    }

    public MarketDataStreamConfiguration build() {
      return new MarketDataStreamConfiguration(stubConstructor, method, call, createMarketDataStreamObserver());
    }

    private MarketDataStreamObserver createMarketDataStreamObserver() {
      return new MarketDataStreamObserver(onResponseListeners, onNextListeners, onErrorListeners, onCompleteListeners);
    }
  }
}
