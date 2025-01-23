package ru.ttech.piapi.core.connector;

import io.github.resilience4j.retry.RetryRegistry;
import io.grpc.Context;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;
import ru.ttech.piapi.core.connector.exception.ServiceRuntimeException;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/**
 * Обёртка для работы с асинхронными gRPC стабами
 */
public class AsyncStubWrapper<S extends AbstractAsyncStub<S>> {

  private final boolean contextFork;
  private final S stub;
  private final RetryRegistry retryRegistry;

  AsyncStubWrapper(S stub, boolean contextFork, RetryRegistry retryRegistry) {
    this.stub = stub;
    this.contextFork = contextFork;
    this.retryRegistry = retryRegistry;
  }

  /**
   * Метод для прямого получения gRPC стаба
   *
   * @return gRPC стаб
   */
  public S getStub() {
    return stub;
  }

  /**
   * Метод для асинхронного вызова метода стаба
   *
   * @param call Вызов метода gRPC стаба с параметрами
   * @return CompletableFuture с результатом вызова метода
   */
  public <T> CompletableFuture<T> callAsyncMethod(BiConsumer<S, StreamObserver<T>> call) {
    // TODO: нужно как-то оптимизировать, чтобы не создавать новый поток для каждого вызова
    var scheduler = Executors.newSingleThreadScheduledExecutor();
    return retryRegistry.retry("invest").executeCompletionStage(scheduler, () -> {
      var cf = new CompletableFuture<T>();
      if (!contextFork) {
        call.accept(stub, mkStreamObserverWithFuture(cf));
        return cf;
      }
      Context forkedContext = Context.current().fork();
      Context origContext = forkedContext.attach();
      try {
        forkedContext.run(() -> call.accept(stub, mkStreamObserverWithFuture(cf)));
      } finally {
        forkedContext.detach(origContext);
      }
      return cf;
    }).toCompletableFuture()
      .whenComplete((r, e) -> scheduler.shutdown());
  }

  private <T> StreamObserver<T> mkStreamObserverWithFuture(CompletableFuture<T> cf) {
    return new ClientCallStreamObserver<>() {
      @Override
      public void cancel(@Nullable String s, @Nullable Throwable throwable) {
      }

      @Override
      public boolean isReady() {
        return false;
      }

      @Override
      public void setOnReadyHandler(Runnable runnable) {
      }

      @Override
      public void disableAutoInboundFlowControl() {
      }

      @Override
      public void request(int i) {
      }

      @Override
      public void setMessageCompression(boolean b) {
      }

      @Override
      public void onNext(T value) {
        cf.complete(value);
      }

      @Override
      public void onError(Throwable throwable) {
        cf.completeExceptionally(new ServiceRuntimeException(throwable));
      }

      @Override
      public void onCompleted() {
      }
    };
  }
}
