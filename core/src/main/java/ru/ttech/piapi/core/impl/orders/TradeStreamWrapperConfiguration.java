package ru.ttech.piapi.core.impl.orders;

import ru.tinkoff.piapi.contract.v1.OrdersStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.ResultSubscriptionStatus;
import ru.tinkoff.piapi.contract.v1.TradesStreamRequest;
import ru.tinkoff.piapi.contract.v1.TradesStreamResponse;
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
 * Конфигурация для обёртки {@link ResilienceServerSideStreamWrapper} над стримом сделок сервиса {@link OrdersStreamServiceGrpc}
 */
public class TradeStreamWrapperConfiguration extends ResilienceServerSideStreamWrapperConfiguration<TradesStreamRequest, TradesStreamResponse> {

  protected TradeStreamWrapperConfiguration(
    ScheduledExecutorService executorService,
    List<OnNextListener<TradesStreamResponse>> onResponseListeners,
    List<Runnable> onConnectListeners
  ) {
    super(executorService, onResponseListeners, onConnectListeners);
  }

  @Override
  public Function<TradesStreamRequest,
    ServerSideStreamConfiguration.Builder<?, TradesStreamRequest, TradesStreamResponse>> getConfigurationBuilder(int pingDelay) {
    return request -> {
      var requestWithPing = TradesStreamRequest.newBuilder(request).setPingDelayMs(pingDelay).build();
      return ServerSideStreamConfiguration.builder(
          OrdersStreamServiceGrpc::newStub,
          OrdersStreamServiceGrpc.getTradesStreamMethod(),
          (stub, observer) -> stub.tradesStream(requestWithPing, observer))
        .addOnNextListener(response -> {
          if (response.hasOrderTrades()) {
            onResponseListeners.forEach(listener -> listener.onNext(response));
          }
        });
    };
  }

  @Override
  public BiFunction<TradesStreamRequest, TradesStreamResponse, Optional<TradesStreamRequest>> getSubscriptionResultProcessor() {
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
   *     var tradesConfig = TradeStreamWrapperConfiguration.builder(executorService)
   *       .addOnResponseListener(trade -> logger.info("Trades update: {}", trade))
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

  public static class Builder extends ResilienceServerSideStreamWrapperConfiguration.Builder<TradesStreamRequest, TradesStreamResponse> {

    protected Builder(ScheduledExecutorService executorService) {
      super(executorService);
    }

    @Override
    public TradeStreamWrapperConfiguration build() {
      return new TradeStreamWrapperConfiguration(executorService, onResponseListeners, onConnectListeners);
    }
  }
}
