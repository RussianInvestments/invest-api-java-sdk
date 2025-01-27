package ru.ttech.piapi.core.connector.resilience;

import io.github.resilience4j.decorators.Decorators;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;
import ru.ttech.piapi.core.connector.AsyncStubWrapper;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Обёртка над {@link AsyncStubWrapper} для поддержки функционала библиотеки resilience4j
 */
public class ResilienceAsyncStubWrapper<S extends AbstractAsyncStub<S>> {

  private final AsyncStubWrapper<S> asyncStubWrapper;
  private final ResilienceConfiguration resilienceConfiguration;

  public ResilienceAsyncStubWrapper(
    AsyncStubWrapper<S> asyncStubWrapper,
    ResilienceConfiguration resilienceConfiguration
  ) {
    this.asyncStubWrapper = asyncStubWrapper;
    this.resilienceConfiguration = resilienceConfiguration;
  }

  /**
   * Метод для асинхронного вызова метода стаба c поддержкой resilience4j
   *
   * <p>Пример вызова: <pre>{@code
   *     CompletableFuture<GetLastPricesResponse> asyncResponse =
   *       resilienceAsyncService.callAsyncMethod(
   *         MarketDataServiceGrpc.getGetLastPricesMethod(),
   *         (stub, observer) -> stub.getLastPrices(request, observer)
   *       );
   * }</pre>
   *
   * @param method Метод gRPC стаба. Необходим для получения настроек resilience
   * @param call   Вызов метода gRPC сервиса с параметрами
   * @return CompletableFuture с результатом вызова метода
   */
  public <T> CompletableFuture<T> callAsyncMethod(
    MethodDescriptor<?, T> method,
    BiConsumer<S, StreamObserver<T>> call
  ) {
    return Decorators.ofCompletionStage(() -> asyncStubWrapper.callAsyncMethod(call))
      .withBulkhead(resilienceConfiguration.getBulkheadForMethod(method))
      .withRateLimiter(resilienceConfiguration.getRateLimiterForMethod(method))
      .withCircuitBreaker(resilienceConfiguration.getCircuitBreakerForMethod(method))
      .withRetry(resilienceConfiguration.getRetryForMethod(method), resilienceConfiguration.getExecutorService())
      .get().toCompletableFuture();
  }

  /**
   * Метод для получния обёртки {@link AsyncStubWrapper}
   *
   * @return Асинхронная обёртка над gRPC сервисом
   */
  public AsyncStubWrapper<S> getAsyncStubWrapper() {
    return asyncStubWrapper;
  }
}
