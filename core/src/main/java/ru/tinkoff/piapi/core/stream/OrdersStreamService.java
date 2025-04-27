package ru.tinkoff.piapi.core.stream;

import io.grpc.Context;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamRequest;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamResponse;
import ru.tinkoff.piapi.contract.v1.OrdersStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.TradesStreamRequest;
import ru.tinkoff.piapi.contract.v1.TradesStreamResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Deprecated(since = "1.30", forRemoval = true)
public class OrdersStreamService {
  private final OrdersStreamServiceGrpc.OrdersStreamServiceStub stub;
  private final Map<String, Runnable> disposeMap = new ConcurrentHashMap<>();

  public OrdersStreamService(@Nonnull OrdersStreamServiceGrpc.OrdersStreamServiceStub stub) {
    this.stub = stub;
  }

  public String subscribeTrades(@Nonnull StreamProcessor<TradesStreamResponse> streamProcessor,
                              @Nullable Consumer<Throwable> onErrorCallback) {
    return tradesStream(streamProcessor, onErrorCallback, Collections.emptyList());
  }

  public void closeStream(String streamKey) {
    disposeMap.computeIfPresent(streamKey, (k, dispose) -> {
      dispose.run();
      return null;
    });
  }

  /**
   * Подписка на стрим сделок
   *
   * @param streamProcessor обработчик пришедших сообщений в стриме
   * @param onErrorCallback обработчик ошибок в стриме
   * @param accounts        Идентификаторы счетов
   */
  public String subscribeTrades(@Nonnull StreamProcessor<TradesStreamResponse> streamProcessor,
                              @Nullable Consumer<Throwable> onErrorCallback,
                              @Nonnull Iterable<String> accounts) {
    return tradesStream(streamProcessor, onErrorCallback, accounts);
  }

  /**
   * Подписка на стрим сделок
   *
   * @param streamProcessor обработчик пришедших сообщений в стриме
   */
  public String subscribeTrades(@Nonnull StreamProcessor<TradesStreamResponse> streamProcessor) {
    return tradesStream(streamProcessor, null, Collections.emptyList());
  }

  /**
   * Подписка на стрим сделок
   *
   * @param streamProcessor обработчик пришедших сообщений в стриме
   * @param accounts        Идентификаторы счетов
   */
  public String subscribeTrades(@Nonnull StreamProcessor<TradesStreamResponse> streamProcessor,
                              @Nonnull Iterable<String> accounts) {
    return tradesStream(streamProcessor, null, accounts);
  }

  private String tradesStream(@Nonnull StreamProcessor<TradesStreamResponse> streamProcessor,
                              @Nullable Consumer<Throwable> onErrorCallback,
                              @Nonnull Iterable<String> accounts) {
    var request = TradesStreamRequest
      .newBuilder()
      .addAllAccounts(accounts)
      .build();

    String streamKey = UUID.randomUUID().toString();
    var context = Context.current().fork().withCancellation();
    disposeMap.put(streamKey, () -> context.cancel(new RuntimeException("canceled by user")));
    context.run(() -> stub.tradesStream(
      request,
      new StreamObserverWithProcessor<>(streamProcessor, onErrorCallback)
    ));

    return streamKey;


  }

  /**
   * Подписка на стрим заявок
   *
   * @param streamProcessor обработчик пришедших сообщений в стриме
   * @param onErrorCallback обработчик ошибок в стриме
   * @param accounts        Идентификаторы счетов
   */
  public String subscribeOrderState(@Nonnull StreamProcessor<OrderStateStreamResponse> streamProcessor,
                                @Nullable Consumer<Throwable> onErrorCallback,
                                @Nonnull Iterable<String> accounts) {
    return orderStateStream(streamProcessor, onErrorCallback, accounts);
  }

  /**
   * Подписка на стрим заявок
   *
   * @param streamProcessor обработчик пришедших сообщений в стриме
   */
  public String subscribeOrderState(@Nonnull StreamProcessor<OrderStateStreamResponse> streamProcessor) {
    return orderStateStream(streamProcessor, null, Collections.emptyList());
  }

  /**
   * Подписка на стрим заявок
   *
   * @param streamProcessor обработчик пришедших сообщений в стриме
   * @param accounts        Идентификаторы счетов
   */
  public String subscribeOrderState(@Nonnull StreamProcessor<OrderStateStreamResponse> streamProcessor,
                                @Nonnull Iterable<String> accounts) {
    return orderStateStream(streamProcessor, null, accounts);
  }

  private String orderStateStream(@Nonnull StreamProcessor<OrderStateStreamResponse> streamProcessor,
                              @Nullable Consumer<Throwable> onErrorCallback,
                              @Nonnull Iterable<String> accounts) {
    var request = OrderStateStreamRequest
      .newBuilder()
      .addAllAccounts(accounts)
      .build();

    String streamKey = UUID.randomUUID().toString();
    var context = Context.current().fork().withCancellation();
    disposeMap.put(streamKey, () -> context.cancel(new RuntimeException("canceled by user")));
    context.run(() -> stub.orderStateStream(
      request,
      new StreamObserverWithProcessor<>(streamProcessor, onErrorCallback)
    ));

    return streamKey;


  }


}
