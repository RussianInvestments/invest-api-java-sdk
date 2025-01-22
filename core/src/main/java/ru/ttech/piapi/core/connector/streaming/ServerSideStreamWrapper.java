package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Context;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public class ServerSideStreamWrapper<S extends AbstractAsyncStub<S>, RespT> {

  private final AtomicReference<Context.CancellableContext> contextRef = new AtomicReference<>();
  private final S stub;
  private final BiConsumer<S, StreamObserver<RespT>> call;
  @SuppressWarnings({"unused", "FieldCanBeLocal"})
  private final MethodDescriptor<?, RespT> method;
  private final StreamResponseObserver<RespT> responseObserver;

  public ServerSideStreamWrapper(
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

  public void disconnect() {
    var context = contextRef.get();
    if (context != null) context.cancel(new RuntimeException("canceled by user"));
  }
}
