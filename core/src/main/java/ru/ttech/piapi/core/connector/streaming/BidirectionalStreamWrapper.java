package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Context;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Обёртка над bidirectional стримом
 */
public class BidirectionalStreamWrapper<S extends AbstractAsyncStub<S>, ReqT, RespT> {

  private final AtomicReference<Context.CancellableContext> contextRef = new AtomicReference<>();
  private final S stub;
  private final BidirectionalStreamConfiguration<S, ReqT, RespT> configuration;
  private final Supplier<StreamObserver<RespT>> responseObserverCreator;
  private StreamObserver<ReqT> requestObserver;

  BidirectionalStreamWrapper(
    S stub,
    BidirectionalStreamConfiguration<S, ReqT, RespT> configuration
  ) {
    this.stub = stub;
    this.configuration = configuration;
    this.responseObserverCreator = configuration.getResponseObserverCreator();
  }

  /**
   * Метод подключения к стриму
   */
  public void connect() {
    var context = Context.current().fork().withCancellation();
    var ctx = context.attach();
    try {
      requestObserver = configuration.getCall().apply(stub.withWaitForReady(), responseObserverCreator.get());
      contextRef.set(context);
    } finally {
      context.detach(ctx);
    }
  }

  /**
   * Метод завершения стрима
   */
  public void disconnect() {
    Optional.ofNullable(contextRef.getAndUpdate(cancellableContext -> null))
      .ifPresent(context -> context.cancel(new RuntimeException("canceled by user")));
  }

  /**
   * Метод для отправки нового запроса в стрим
   *
   * @param request Запрос <p>Можно подписаться или отписаться от каких-либо обновлений</p>
   */
  public void newCall(ReqT request) {
    requestObserver.onNext(request);
  }
}
