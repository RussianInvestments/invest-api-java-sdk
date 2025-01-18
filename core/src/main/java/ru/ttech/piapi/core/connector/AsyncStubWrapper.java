package ru.ttech.piapi.core.connector;

import io.grpc.Context;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;
import ru.ttech.piapi.core.connector.exception.ServiceRuntimeException;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Обёртка для работы с асинхронными gRPC стабами
 */
public class AsyncStubWrapper<S extends AbstractAsyncStub<S>> {

  private final boolean contextFork;
  private final S stub;

  AsyncStubWrapper(S stub, boolean contextFork) {
    this.stub = stub;
    this.contextFork = contextFork;
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
    // TODO: добавить поддержку cancel
    var cf = new CompletableFuture<T>();
    if (!contextFork) {
      call.accept(stub, mkStreamObserverWithFuture(cf));
      return cf;
    }
    Context forkedContext = Context.current().fork();
    Context origContext = forkedContext.attach();
    try {
      call.accept(stub, mkStreamObserverWithFuture(cf));
    } catch (Throwable e) {
      cf.completeExceptionally(new ServiceRuntimeException(e));
    } finally {
      forkedContext.detach(origContext);
    }
    return cf;
  }

  private <T> StreamObserver<T> mkStreamObserverWithFuture(CompletableFuture<T> cf) {
    return new ClientCallStreamObserver<>() {
      @Override
      public void cancel(@Nullable String s, @Nullable Throwable throwable) {
        // TODO:
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
