package ru.ttech.piapi.core.impl.operations;

import ru.tinkoff.piapi.contract.v1.OperationsStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.PositionsAccountSubscriptionStatus;
import ru.tinkoff.piapi.contract.v1.PositionsStreamRequest;
import ru.tinkoff.piapi.contract.v1.PositionsStreamResponse;
import ru.tinkoff.piapi.contract.v1.PositionsSubscriptionStatus;
import ru.ttech.piapi.core.connector.resilience.ResilienceServerSideStreamWrapper;
import ru.ttech.piapi.core.connector.resilience.ResilienceServerSideStreamWrapperConfiguration;
import ru.ttech.piapi.core.connector.streaming.ServerSideStreamConfiguration;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Конфигурация для обёртки {@link ResilienceServerSideStreamWrapper} над стримом позиций сервиса {@link OperationsStreamServiceGrpc}
 */
public class PositionsStreamWrapperConfiguration extends ResilienceServerSideStreamWrapperConfiguration<PositionsStreamRequest, PositionsStreamResponse> {

  protected PositionsStreamWrapperConfiguration(
    ScheduledExecutorService executorService,
    List<OnNextListener<PositionsStreamResponse>> onResponseListeners,
    List<Runnable> onConnectListeners
  ) {
    super(executorService, onResponseListeners, onConnectListeners);
  }

  @Override
  public Function<PositionsStreamRequest,
    ServerSideStreamConfiguration.Builder<?, PositionsStreamRequest, PositionsStreamResponse>> getConfigurationBuilder(int pingDelay) {
    return null;
  }

  @Override
  public BiFunction<PositionsStreamRequest, PositionsStreamResponse, Optional<PositionsStreamRequest>> getSubscriptionResultProcessor() {
    return (request, response) -> {
      if (response.hasSubscriptions()) {
        var successSubscriptionsList = response.getSubscriptions().getAccountsList().stream()
          .filter(subscription -> subscription.getSubscriptionStatus() == PositionsAccountSubscriptionStatus.POSITIONS_SUBSCRIPTION_STATUS_SUCCESS)
          .collect(Collectors.toList());
        if (!successSubscriptionsList.isEmpty()) {
          var filteredRequest = request.getAccountsList().size() == successSubscriptionsList.size()
            ? request
            : PositionsStreamRequest.newBuilder(request).clearAccounts()
            .addAllAccounts(successSubscriptionsList.stream().map(PositionsSubscriptionStatus::getAccountId).collect(Collectors.toList()))
            .build();
          return Optional.of(filteredRequest);
        }
      }
      return Optional.empty();
    };
  }

  /**
   * Фабричный метод получения нового билдера для создания конфигурации.
   * <p>Пример вызова:<pre>{@code
   *     var positionsConfig = PositionsStreamWrapperConfiguration.builder(executorService)
   *       .addOnResponseListener(positions -> logger.info("Positions update: {}", positions))
   *       .addOnConnectListener(() -> logger.info("Stream connected!"))
   *       .build();
   * }</pre>
   *
   * @param executorService поток для проверки состояния соединения
   * @return Билдер конфигурации обёртки над стримом
   */
  public static Builder builder(ScheduledExecutorService executorService) {
    return new Builder(executorService);
  }

  public static class Builder extends ResilienceServerSideStreamWrapperConfiguration.Builder<PositionsStreamRequest, PositionsStreamResponse> {

    protected Builder(ScheduledExecutorService executorService) {
      super(executorService);
    }

    @Override
    public ResilienceServerSideStreamWrapperConfiguration<PositionsStreamRequest, PositionsStreamResponse> build() {
      return new PositionsStreamWrapperConfiguration(executorService, onResponseListeners, onConnectListeners);
    }
  }
}
