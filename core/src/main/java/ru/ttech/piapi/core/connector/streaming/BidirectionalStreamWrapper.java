package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Context;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

/**
 * Обёртка над bidirectional стримом
 */
public class BidirectionalStreamWrapper<S extends AbstractAsyncStub<S>, ReqT, RespT> {

  private final AtomicReference<Context.CancellableContext> contextRef = new AtomicReference<>();
  private final S stub;
  private final BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> call;
  @SuppressWarnings({"unused", "FieldCanBeLocal"})
  private final MethodDescriptor<ReqT, RespT> method;
  private final StreamResponseObserver<RespT> responseObserver;
  private StreamObserver<ReqT> requestObserver;

  BidirectionalStreamWrapper(
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

  /**
   * Метод подключения к стриму
   */
  public void connect() {
    var context = Context.current().fork().withCancellation();
    var ctx = context.attach();
    try {
      requestObserver = call.apply(stub, responseObserver);
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
    if (context != null) context.cancel(new RuntimeException("canceled by user"));
  }

  /**
   * Метод для отправки нового запроса в стрим
   * @param request Запрос <p>Можно подписаться или отписаться от каких-либо обновлений</p>
   */
  public void newCall(ReqT request) {
    requestObserver.onNext(request);
  }
}
