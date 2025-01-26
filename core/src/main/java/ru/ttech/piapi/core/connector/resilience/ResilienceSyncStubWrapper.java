package ru.ttech.piapi.core.connector.resilience;

import io.github.resilience4j.decorators.Decorators;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractBlockingStub;
import ru.ttech.piapi.core.connector.SyncStubWrapper;

import java.util.function.Function;

/**
 * Обёртка над {@link SyncStubWrapper} для поддержки функционала библиотеки resilience4j
 */
public class ResilienceSyncStubWrapper<S extends AbstractBlockingStub<S>> {

  private final SyncStubWrapper<S> syncStubWrapper;
  private final ResilienceConfiguration resilienceConfiguration;

  public ResilienceSyncStubWrapper(
    SyncStubWrapper<S> syncStubWrapper,
    ResilienceConfiguration resilienceConfiguration
  ) {
    this.syncStubWrapper = syncStubWrapper;
    this.resilienceConfiguration = resilienceConfiguration;
  }

  /**
   * Метод для синхронного вызова метода стаба c поддержкой resilience4j
   *
   * <p>Пример вызова: <pre>{@code
   * var response = resilienceSyncService.callSyncMethod(
   *       MarketDataServiceGrpc.getGetLastPricesMethod(),
   *       stub -> stub.getLastPrices(request)
   *     );
   * }</pre>
   *
   * @param method Метод gRPC стаба. Необходим для получения настроек resilience
   * @param call   Вызов метода gRPC сервиса с параметрами
   * @return CompletableFuture с результатом вызова метода
   */
  public <T> T callSyncMethod(MethodDescriptor<?, T> method, Function<S, T> call) {
    return Decorators.ofSupplier(() -> syncStubWrapper.callSyncMethod(call))
      .withRateLimiter(resilienceConfiguration.getRateLimiterForMethod(method))
      .withCircuitBreaker(resilienceConfiguration.getCircuitBreakerForMethod(method))
      .withRetry(resilienceConfiguration.getRetryForMethod(method))
      .get();
  }

  /**
   * Метод для получния обёртки {@link SyncStubWrapper}
   *
   * @return Синхронная обёртка над gRPC сервисом
   */
  public SyncStubWrapper<S> getSyncStubWrapper() {
    return syncStubWrapper;
  }
}
