package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Context;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Обёртка над server-side стримом
 */
public class ServerSideStreamWrapper<S extends AbstractAsyncStub<S>, RespT> {

  private final AtomicReference<Context.CancellableContext> contextRef = new AtomicReference<>();
  private final S stub;
  private final BiConsumer<S, StreamObserver<RespT>> call;
  @SuppressWarnings({"unused", "FieldCanBeLocal"})
  private final MethodDescriptor<?, RespT> method;
  private final StreamResponseObserver<RespT> responseObserver;

  ServerSideStreamWrapper(
    S stub,
    MethodDescriptor<?, RespT> method,
    BiConsumer<S, StreamObserver<RespT>> call,
    StreamResponseObserver<RespT> responseObserver
  ) {
    this.stub = stub;
    this.method = method;
    this.call = call;
    this.responseObserver = responseObserver;
  }

  /**
   * Метод подключения к стриму
   */
  public void connect() {
    var context = Context.current().fork().withCancellation();
    var ctx = context.attach();
    try {
      call.accept(stub, responseObserver);
      contextRef.set(context);
    } finally {
      context.detach(ctx);
    }
  }

  /**
   * Метод завершения стрима
   */
  public void disconnect() {
    var context = contextRef.get();
    if (isConnected()) {
      context.cancel(new RuntimeException("canceled by user"));
      contextRef.set(null);
    }
  }

  public boolean isConnected() {
    return contextRef.get()!= null;
  }

  public StreamResponseObserver<RespT> getResponseObserver() {
    return responseObserver;
  }
}
