package ru.ttech.piapi.core.connector.resilience;

import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;
import ru.ttech.piapi.core.connector.AsyncStubWrapper;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

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

  public <T> CompletableFuture<T> callAsyncMethod(
    MethodDescriptor<?, T> method,
    BiConsumer<S, StreamObserver<T>> call
  ) {
    return resilienceConfiguration.getRetryForMethod(method)
      .executeCompletionStage(
        resilienceConfiguration.getScheduledExecutorService(),
        () -> asyncStubWrapper.callAsyncMethod(call)
        )
      .toCompletableFuture();
  }

  public AsyncStubWrapper<S> getAsyncStubWrapper() {
    return asyncStubWrapper;
  }
}
