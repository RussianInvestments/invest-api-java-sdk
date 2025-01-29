package ru.ttech.piapi.core.impl.marketdata;

import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.StreamObserver;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.contract.v1.MarketDataStreamServiceGrpc;
import ru.ttech.piapi.core.connector.streaming.BidirectionalStreamConfiguration;
import ru.ttech.piapi.core.connector.streaming.BidirectionalStreamWrapper;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.LastPriceWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.OrderBookWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.TradeWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.TradingStatusWrapper;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Типизированная конфигурация для обёртки {@link BidirectionalStreamWrapper} над {@link MarketDataStreamServiceGrpc}
 */
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

  /**
   * Фабричный метод получения билдера для создания конфигурации обёртки MarketData стрима
   * <p>Пример вызова:<pre>{@code
   * var marketDataStreamConfiguration = MarketDataStreamConfiguration.builder()
   *         .addOnCandleListener(candle -> logger.info("Свеча: {}", candle))
   *         .addOnLastPriceListener(lastPrice -> logger.info("Цена: {}", lastPrice))
   *         .addOnTradeListener(trade -> logger.info("Сделка: {}", trade))
   *         .addOnNextListener(response -> logger.info("Сообщение: {}", response))
   *         .addOnErrorListener(e -> logger.error("Произошла ошибка: {}", e.getMessage()))
   *         .addOnCompleteListener(() -> logger.info("Стрим завершен"))
   *         .build()
   * }</pre>
   *
   * @return Объект билдера конфигурации обёртки стрима
   */
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

    /**
     * Метод добавления листенера для обработки {@link CandleWrapper}
     *
     * @param onCandleListener Листенер для обработки {@link CandleWrapper}
     *                           <p>Можно задать в виде лямбы: <pre>{@code
     *                           candle -> log.info("{}", candle)
     *                           }</pre>
     * @return Билдер конфигурации обёртки над стримом
     */
    public Builder addOnCandleListener(OnNextListener<CandleWrapper> onCandleListener) {
      return addMarketDataResponseListener(
        MarketDataResponseType.CANDLE,
        response -> onCandleListener.onNext(new CandleWrapper(response.getCandle()))
      );
    }

    /**
     * Метод добавления листенера для обработки {@link LastPriceWrapper}
     *
     * @param onLastPriceListener Листенер для обработки {@link LastPriceWrapper}
     *                           <p>Можно задать в виде лямбы: <pre>{@code
     *                           lastPrice -> log.info("{}", lastPrice)
     *                           }</pre>
     * @return Билдер конфигурации обёртки над стримом
     */
    public Builder addOnLastPriceListener(OnNextListener<LastPriceWrapper> onLastPriceListener) {
      return addMarketDataResponseListener(
        MarketDataResponseType.LAST_PRICE,
        response -> onLastPriceListener.onNext(new LastPriceWrapper(response.getLastPrice()))
      );
    }

    /**
     * Метод добавления листенера для обработки {@link OrderBookWrapper}
     *
     * @param onOrderBookListener Листенер для обработки {@link OrderBookWrapper}
     *                           <p>Можно задать в виде лямбы: <pre>{@code
     *                           orderBook -> log.info("{}", orderBook)
     *                           }</pre>
     * @return Билдер конфигурации обёртки над стримом
     */
    public Builder addOnOrderBookListener(OnNextListener<OrderBookWrapper> onOrderBookListener) {
      return addMarketDataResponseListener(
        MarketDataResponseType.ORDER_BOOK,
        response -> onOrderBookListener.onNext(new OrderBookWrapper(response.getOrderbook()))
      );
    }

    /**
     * Метод добавления листенера для обработки {@link TradeWrapper}
     *
     * @param onTradeListener Листенер для обработки {@link TradingStatusWrapper}
     *                           <p>Можно задать в виде лямбы: <pre>{@code
     *                           trade -> log.info("{}", trade)
     *                           }</pre>
     * @return Билдер конфигурации обёртки над стримом
     */
    public Builder addOnTradeListener(OnNextListener<TradeWrapper> onTradeListener) {
      return addMarketDataResponseListener(
        MarketDataResponseType.TRADE,
        response -> onTradeListener.onNext(new TradeWrapper(response.getTrade()))
      );
    }

    /**
     * Метод добавления листенера для обработки {@link TradingStatusWrapper}
     *
     * @param onTradingStatusListener Листенер для обработки {@link TradingStatusWrapper}
     *                           <p>Можно задать в виде лямбы: <pre>{@code
     *                           tradingStatus -> log.info("{}", tradingStatus)
     *                           }</pre>
     * @return Билдер конфигурации обёртки над стримом
     */
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
      onResponseListeners.computeIfAbsent(responseType, __ -> new LinkedList<>()).add(onNextListener);
      return this;
    }

    /**
     * Метод для создания конфигурации для {@link BidirectionalStreamWrapper}
     *
     * @return Конфигурация для {@link BidirectionalStreamWrapper}
     */
    public MarketDataStreamConfiguration build() {
      return new MarketDataStreamConfiguration(stubConstructor, method, call, createMarketDataStreamObserver());
    }

    private MarketDataStreamObserver createMarketDataStreamObserver() {
      return new MarketDataStreamObserver(onResponseListeners, onNextListeners, onErrorListeners, onCompleteListeners);
    }
  }
}
