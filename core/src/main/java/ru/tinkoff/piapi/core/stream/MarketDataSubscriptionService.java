package ru.tinkoff.piapi.core.stream;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import ru.tinkoff.piapi.contract.v1.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static ru.tinkoff.piapi.core.utils.DefaultValues.DEFAULT_SUBSCRIPTION_INTERVAL;

public class MarketDataSubscriptionService {
  private final StreamObserver<MarketDataRequest> observer;
  private final AtomicReference<Context.CancellableContext> contextRef = new AtomicReference<>();
  private static final SubscriptionAction ACTION_SUBSCRIBE =  SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE;
  private static final SubscriptionAction ACTION_UNSUBSCRIBE =  SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE;

  public MarketDataSubscriptionService(
    @Nonnull MarketDataStreamServiceGrpc.MarketDataStreamServiceStub stub,
    @Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
    @Nullable Consumer<Throwable> onErrorCallback) {
    var context = Context.current().fork().withCancellation();
    var ctx = context.attach();
    try {
      this.observer = stub.marketDataStream(new StreamObserverWithProcessor<>(streamProcessor, onErrorCallback));
      contextRef.set(context);
    } finally {
      context.detach(ctx);
    }
  }

  public void subscribeTrades(@Nonnull List<String> instrumentIds) {
    tradesStream(instrumentIds, ACTION_SUBSCRIBE, null);
  }

  public void unsubscribeTrades(@Nonnull List<String> instrumentIds) {
    tradesStream(instrumentIds, ACTION_UNSUBSCRIBE, null);
  }

  public void subscribeTrades(@Nonnull TradeSourceType tradeSourceType, @Nonnull List<String> instrumentIds) {
    tradesStream(instrumentIds, ACTION_SUBSCRIBE, tradeSourceType);
  }

  public void unsubscribeTrades(@Nonnull TradeSourceType tradeSourceType, @Nonnull List<String> instrumentIds) {
    tradesStream(instrumentIds, ACTION_UNSUBSCRIBE, tradeSourceType);
  }

  public void subscribeOrderbook(@Nonnull List<String> instrumentIds) {
    subscribeOrderbook(instrumentIds, 1);
  }

  public void subscribeOrderbook(@Nonnull List<String> instrumentIds, int depth) {
    orderBookStream(instrumentIds, ACTION_SUBSCRIBE, depth);
  }

  public void unsubscribeOrderbook(@Nonnull List<String> instrumentIds) {
    unsubscribeOrderbook(instrumentIds, 1);
  }

  public void unsubscribeOrderbook(@Nonnull List<String> instrumentIds, int depth) {
    orderBookStream(instrumentIds, ACTION_UNSUBSCRIBE, depth);
  }

  public void subscribeInfo(@Nonnull List<String> instrumentIds) {
    infoStream(instrumentIds, ACTION_SUBSCRIBE);
  }

  public void unsubscribeInfo(@Nonnull List<String> instrumentIds) {
    infoStream(instrumentIds, ACTION_UNSUBSCRIBE);
  }

  /**
   * Подписка на свечи с интервалом {@link ru.tinkoff.piapi.core.utils.DefaultValues#DEFAULT_SUBSCRIPTION_INTERVAL}.
   * @param instrumentIds перечень идентификаторов инструментов
   */
  public void subscribeCandles(@Nonnull List<String> instrumentIds) {
    subscribeCandles(instrumentIds, false);
  }

  /**
   * Подписка на свечи с интервалом {@link ru.tinkoff.piapi.core.utils.DefaultValues#DEFAULT_SUBSCRIPTION_INTERVAL}.
   * @param instrumentIds перечень идентификаторов инструментов
   * @param waitingClose получение свечи только после закрытия временного интервала
   */
  public void subscribeCandles(@Nonnull List<String> instrumentIds, boolean waitingClose) {
    subscribeCandles(instrumentIds, DEFAULT_SUBSCRIPTION_INTERVAL, waitingClose);
  }

  /**
   * Подписка на свечи с указанным интервалом.
   * @param instrumentIds перечень идентификаторов инструментов
   * @param interval интервал свечи
   */
  public void subscribeCandles(@Nonnull List<String> instrumentIds, SubscriptionInterval interval) {
    subscribeCandles(instrumentIds, interval, false);
  }

  /**
   * Подписка на свечи с указанным интервалом.
   * @param instrumentIds перечень идентификаторов инструментов
   * @param interval интервал свечи
   * @param waitingClose получение свечи только после закрытия временного интервала
   */
  public void subscribeCandles(@Nonnull List<String> instrumentIds, SubscriptionInterval interval, boolean waitingClose) {
    candlesStream(instrumentIds, ACTION_SUBSCRIBE, interval, waitingClose);
  }

  /**
   * Запрос списка текущих подписок.
   */
  public void mySubscriptions() {
    var builder = GetMySubscriptions
      .newBuilder();
    var request = MarketDataRequest
      .newBuilder()
      .setGetMySubscriptions(builder)
      .build();
    observer.onNext(request);
  }

  public void unsubscribeCandles(@Nonnull List<String> instrumentIds) {
    unsubscribeCandles(instrumentIds, DEFAULT_SUBSCRIPTION_INTERVAL);
  }

  public void unsubscribeCandles(@Nonnull List<String> instrumentIds, SubscriptionInterval interval) {
    candlesStream(instrumentIds, ACTION_UNSUBSCRIBE, interval, false);
  }

  public void subscribeLastPrices(@Nonnull List<String> instrumentIds) {
    lastPricesStream(instrumentIds, ACTION_SUBSCRIBE);
  }

  public void unsubscribeLastPrices(@Nonnull List<String> instrumentIds) {
    lastPricesStream(instrumentIds, ACTION_UNSUBSCRIBE);
  }

  public void cancel() {
    var context = contextRef.get();
    if (context != null) context.cancel(new RuntimeException("canceled by user"));
  }


  private void candlesStream(@Nonnull List<String> instrumentIds,
                             @Nonnull SubscriptionAction action,
                             @Nonnull SubscriptionInterval interval,
                             boolean waitingClose) {
    var builder = SubscribeCandlesRequest
      .newBuilder()
      .setSubscriptionAction(action)
      .setWaitingClose(waitingClose);
    for (var instrumentId : instrumentIds) {
      builder.addInstruments(CandleInstrument
        .newBuilder()
        .setInterval(interval)
        .setInstrumentId(instrumentId)
        .build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeCandlesRequest(builder)
      .build();
    observer.onNext(request);
  }

  private void lastPricesStream(@Nonnull List<String> instrumentIds,
                                @Nonnull SubscriptionAction action) {
    var builder = SubscribeLastPriceRequest
      .newBuilder()
      .setSubscriptionAction(action);
    for (var instrumentId : instrumentIds) {
      builder.addInstruments(LastPriceInstrument
        .newBuilder()
        .setInstrumentId(instrumentId)
        .build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeLastPriceRequest(builder)
      .build();
    observer.onNext(request);
  }

  private void tradesStream(@Nonnull List<String> instrumentIds,
                            @Nonnull SubscriptionAction action,
                            @Nullable TradeSourceType tradeSourceType) {
    var builder = SubscribeTradesRequest
      .newBuilder()
      .setSubscriptionAction(action);
    if (tradeSourceType != null) {
      builder.setTradeSource(tradeSourceType);
    }
    for (String instrumentId : instrumentIds) {
      builder.addInstruments(TradeInstrument
        .newBuilder()
        .setInstrumentId(instrumentId)
        .build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeTradesRequest(builder)
      .build();
    observer.onNext(request);
  }

  private void orderBookStream(@Nonnull List<String> instrumentIds,
                               @Nonnull SubscriptionAction action,
                               int depth) {
    var builder = SubscribeOrderBookRequest
      .newBuilder()
      .setSubscriptionAction(action);
    for (var instrumentId : instrumentIds) {
      builder.addInstruments(OrderBookInstrument
        .newBuilder()
        .setDepth(depth)
        .setInstrumentId(instrumentId)
        .build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeOrderBookRequest(builder)
      .build();
    observer.onNext(request);
  }

  private void infoStream(@Nonnull List<String> instrumentIds,
                          @Nonnull SubscriptionAction action) {
    var builder = SubscribeInfoRequest
      .newBuilder()
      .setSubscriptionAction(action);
    for (var instrumentId : instrumentIds) {
      builder.addInstruments(InfoInstrument.newBuilder().setInstrumentId(instrumentId).build());
    }
    var request = MarketDataRequest
      .newBuilder()
      .setSubscribeInfoRequest(builder)
      .build();
    observer.onNext(request);
  }
}
