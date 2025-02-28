package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Context;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Обёртка над server-side стримом
 */
public class ServerSideStreamWrapper<S extends AbstractAsyncStub<S>, RespT> {

  private static final Logger logger = LoggerFactory.getLogger(ServerSideStreamWrapper.class);
  private final AtomicReference<Context.CancellableContext> contextRef = new AtomicReference<>();
  private final S stub;
  private final ServerSideStreamConfiguration<S, ?, RespT> configuration;
  private final Supplier<StreamObserver<RespT>> observerCreator;

  ServerSideStreamWrapper(
    S stub,
    ServerSideStreamConfiguration<S, ?, RespT> configuration
  ) {
    this.stub = stub;
    this.configuration = configuration;
    this.observerCreator = configuration.getResponseObserverCreator();
  }

  /**
   * Метод подключения к стриму
   */
  public void connect() {
    var context = Context.current().fork().withCancellation();
    var ctx = context.attach();
    try {
      logger.info("Stream connecting...");
      configuration.getCall().accept(stub, observerCreator.get());
      contextRef.set(context);
      logger.info("Stream connected!");
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
}
