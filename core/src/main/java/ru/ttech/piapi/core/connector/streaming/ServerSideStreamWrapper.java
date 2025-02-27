package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Context;
import io.grpc.stub.AbstractAsyncStub;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Обёртка над server-side стримом
 */
public class ServerSideStreamWrapper<S extends AbstractAsyncStub<S>, RespT> {

  private final AtomicReference<Context.CancellableContext> contextRef = new AtomicReference<>();
  private final S stub;
  private final ServerSideStreamConfiguration<S, ?, RespT> configuration;

  ServerSideStreamWrapper(
    S stub,
    ServerSideStreamConfiguration<S, ?, RespT> configuration
  ) {
    this.stub = stub;
    this.configuration = configuration;
  }

  /**
   * Метод подключения к стриму
   */
  public void connect() {
    var context = Context.current().fork().withCancellation();
    var ctx = context.attach();
    try {
      configuration.getCall().accept(stub, configuration.createResponseObserver());
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
}
