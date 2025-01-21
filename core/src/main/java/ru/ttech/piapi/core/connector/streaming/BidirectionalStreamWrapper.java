package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Context;
import io.grpc.MethodDescriptor;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

public class BidirectionalStreamWrapper<S, ReqT, RespT> {

  private final AtomicReference<Context.CancellableContext> contextRef = new AtomicReference<>();
  private final S stub;
  private final BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> call;
  @SuppressWarnings({"unused", "FieldCanBeLocal"})
  private final MethodDescriptor<ReqT, RespT> method;
  private final StreamResponseObserver<RespT> responseObserver;
  private StreamObserver<ReqT> requestObserver;

  public BidirectionalStreamWrapper(
    S stub,
    MethodDescriptor<ReqT, RespT> method,
    BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> call,
    StreamResponseObserver<RespT> responseObserver
  ) {
    this.stub = stub;
    this.method = method;
    this.call = call;
    this.responseObserver = responseObserver;
  }

  public void subscribe() {
    var context = Context.current().fork().withCancellation();
    var ctx = context.attach();
    try {
      requestObserver = call.apply(stub, responseObserver);
      contextRef.set(context);
    } finally {
      context.detach(ctx);
    }
  }

  public void newCall(ReqT request) {
    requestObserver.onNext(request);
  }
}
