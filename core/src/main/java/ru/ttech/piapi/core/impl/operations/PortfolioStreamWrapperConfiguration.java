package ru.ttech.piapi.core.impl.operations;

import ru.tinkoff.piapi.contract.v1.AccountSubscriptionStatus;
import ru.tinkoff.piapi.contract.v1.OperationsStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.PingDelaySettings;
import ru.tinkoff.piapi.contract.v1.PortfolioStreamRequest;
import ru.tinkoff.piapi.contract.v1.PortfolioStreamResponse;
import ru.tinkoff.piapi.contract.v1.PortfolioSubscriptionStatus;
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
 * Конфигурация для обёртки {@link ResilienceServerSideStreamWrapper} над стримом портфеля сервиса {@link OperationsStreamServiceGrpc}
 */
public class PortfolioStreamWrapperConfiguration extends ResilienceServerSideStreamWrapperConfiguration<PortfolioStreamRequest, PortfolioStreamResponse> {

  protected PortfolioStreamWrapperConfiguration(
    ScheduledExecutorService executorService,
    List<OnNextListener<PortfolioStreamResponse>> onResponseListeners,
    List<Runnable> onConnectListeners
  ) {
    super(executorService, onResponseListeners, onConnectListeners);
  }

  @Override
  public Function<PortfolioStreamRequest,
    ServerSideStreamConfiguration.Builder<?, PortfolioStreamRequest, PortfolioStreamResponse>> getConfigurationBuilder(int pingDelay) {
    return request -> {
      var requestWithPing = PortfolioStreamRequest.newBuilder(request)
        .setPingSettings(PingDelaySettings.newBuilder().setPingDelayMs(pingDelay).build())
        .build();
      return ServerSideStreamConfiguration.builder(
          OperationsStreamServiceGrpc::newStub,
          OperationsStreamServiceGrpc.getPortfolioStreamMethod(),
          (stub, observer) -> stub.portfolioStream(requestWithPing, observer))
        .addOnNextListener(response -> {
          if (response.hasPortfolio()) {
            onResponseListeners.forEach(listener -> listener.onNext(response));
          }
        });
    };
  }

  @Override
  public BiFunction<PortfolioStreamRequest, PortfolioStreamResponse, Optional<PortfolioStreamRequest>> getSubscriptionResultProcessor() {
    return (request, response) -> {
      if (response.hasSubscriptions()) {
        var successSubscriptionsList = response.getSubscriptions().getAccountsList().stream()
          .filter(subscription -> subscription.getSubscriptionStatus() == PortfolioSubscriptionStatus.PORTFOLIO_SUBSCRIPTION_STATUS_SUCCESS)
          .collect(Collectors.toList());
        if (!successSubscriptionsList.isEmpty()) {
          var filteredRequest = request.getAccountsList().size() == successSubscriptionsList.size()
            ? request
            : PortfolioStreamRequest.newBuilder(request).clearAccounts()
            .addAllAccounts(successSubscriptionsList.stream().map(AccountSubscriptionStatus::getAccountId).collect(Collectors.toList()))
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
   *     var portfolioConfig = PortfolioStreamWrapperConfiguration.builder(executorService)
   *       .addOnResponseListener(portfolio -> logger.info("Portfolio update: {}", portfolio))
   *       .addOnConnectListener(() -> logger.info("Stream connected!"))
   *       .build();
   * }</pre>
   * @param executorService поток для проверки состояния соединения
   * @return Билдер конфигурации обёртки над стримом
   */
  public static Builder builder(ScheduledExecutorService executorService) {
    return new Builder(executorService);
  }

  public static class Builder extends ResilienceServerSideStreamWrapperConfiguration.Builder<
    PortfolioStreamRequest, PortfolioStreamResponse> {

    protected Builder(ScheduledExecutorService executorService) {
      super(executorService);
    }

    @Override
    public PortfolioStreamWrapperConfiguration build() {
      return new PortfolioStreamWrapperConfiguration(executorService, onResponseListeners, onConnectListeners);
    }
  }
}
