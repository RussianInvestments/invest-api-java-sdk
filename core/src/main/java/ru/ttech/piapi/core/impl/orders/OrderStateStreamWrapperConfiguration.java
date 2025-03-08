package ru.ttech.piapi.core.impl.orders;

import ru.tinkoff.piapi.contract.v1.OrderStateStreamRequest;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamResponse;
import ru.tinkoff.piapi.contract.v1.OrdersStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.ResultSubscriptionStatus;
import ru.ttech.piapi.core.connector.resilience.ResilienceServerSideStreamWrapper;
import ru.ttech.piapi.core.connector.resilience.ResilienceServerSideStreamWrapperConfiguration;
import ru.ttech.piapi.core.connector.streaming.ServerSideStreamConfiguration;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Конфигурация для обёртки {@link ResilienceServerSideStreamWrapper} над стримом ордеров сервиса {@link OrdersStreamServiceGrpc}
 */
public class OrderStateStreamWrapperConfiguration extends ResilienceServerSideStreamWrapperConfiguration<OrderStateStreamRequest, OrderStateStreamResponse> {

  protected OrderStateStreamWrapperConfiguration(
    ScheduledExecutorService executorService,
    List<OnNextListener<OrderStateStreamResponse>> onResponseListeners,
    List<Runnable> onConnectListeners
  ) {
    super(executorService, onResponseListeners, onConnectListeners);
  }

  @Override
  public Function<OrderStateStreamRequest,
    ServerSideStreamConfiguration.Builder<?, OrderStateStreamRequest, OrderStateStreamResponse>> getConfigurationBuilder(int pingDelay) {
    return request -> {
      var requestWithPing = OrderStateStreamRequest.newBuilder(request).setPingDelayMillis(pingDelay).build();
      return ServerSideStreamConfiguration.builder(
          OrdersStreamServiceGrpc::newStub,
          OrdersStreamServiceGrpc.getOrderStateStreamMethod(),
          (stub, observer) -> stub.orderStateStream(requestWithPing, observer))
        .addOnNextListener(response -> {
          if (response.hasOrderState()) {
            onResponseListeners.forEach(listener -> listener.onNext(response));
          }
        });
    };
  }

  @Override
  public BiFunction<OrderStateStreamRequest, OrderStateStreamResponse, Optional<OrderStateStreamRequest>> getSubscriptionResultProcessor() {
    return (request, response) -> {
      if (response.hasSubscription()
        && response.getSubscription().getStatus() == ResultSubscriptionStatus.RESULT_SUBSCRIPTION_STATUS_OK) {
        return Optional.of(request);
      }
      return Optional.empty();
    };
  }

  /**
   * Фабричный метод получения нового билдера для создания конфигурации.
   * <p>Пример вызова:<pre>{@code
   *     var orderStateConfig = OrderStateStreamWrapperConfiguration.builder(executorService)
   *       .addOnResponseListener(order -> logger.info("Orders update: {}", order))
   *       .addOnConnectListener(() -> logger.info("Stream connected!"))
   *       .build();
   * }</pre></p>
   *
   * @param executorService поток для проверки состояния соединения
   * @return Билдер конфигурации обёртки над стримом
   */
  public static Builder builder(ScheduledExecutorService executorService) {
    return new Builder(executorService);
  }

  public static class Builder extends ResilienceServerSideStreamWrapperConfiguration.Builder<OrderStateStreamRequest, OrderStateStreamResponse> {

    protected Builder(ScheduledExecutorService executorService) {
      super(executorService);
    }

    @Override
    public OrderStateStreamWrapperConfiguration build() {
      return new OrderStateStreamWrapperConfiguration(executorService, onResponseListeners, onConnectListeners);
    }
  }
}
