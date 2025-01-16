package ru.tinkoff.piapi.core.connector;

import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;
import ru.tinkoff.piapi.core.connector.exception.ServiceRuntimeException;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class AsyncStubWrapper<S extends AbstractAsyncStub<S>> {

  private final S stub;

  public AsyncStubWrapper(S stub) {
    this.stub = stub;
  }

  public S getStub() {
    return stub;
  }

  public <T> CompletableFuture<T> callAsyncMethod(BiConsumer<S, StreamObserver<T>> call) {
    var cf = new CompletableFuture<T>();
    call.accept(stub, mkStreamObserverWithFuture(cf));
    return cf;
  }

  private <T> StreamObserver<T> mkStreamObserverWithFuture(CompletableFuture<T> cf) {
    return new StreamObserver<>() {
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
