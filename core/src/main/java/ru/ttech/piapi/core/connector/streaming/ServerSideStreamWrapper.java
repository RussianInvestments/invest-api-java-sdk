package ru.ttech.piapi.core.connector.streaming;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Context;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
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
      configuration.getCall().accept(stub.withWaitForReady(), observerCreator.get());
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

  class DisconnectInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, ResponseT> ClientCall<ReqT, ResponseT> interceptCall(
      MethodDescriptor<ReqT, ResponseT> method, CallOptions callOptions, Channel next
    ) {
      ClientCall<ReqT, ResponseT> call = next.newCall(method, callOptions);
      return new ForwardingClientCall.SimpleForwardingClientCall<>(call) {
        @Override
        public void start(Listener<ResponseT> responseListener, Metadata headers) {
          Listener<ResponseT> listener = new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(responseListener) {
            @Override
            public void onClose(Status status, Metadata trailers) {
              if (status.getCode() == Status.Code.UNAVAILABLE ||
                status.getCode() == Status.Code.UNKNOWN ||
                status.getCode() == Status.Code.DEADLINE_EXCEEDED) {
                logger.error("Соединение потеряно: {}", status.getDescription());
              }
              super.onClose(status, trailers);
            }
          };
          super.start(listener, headers);
        }
      };
    }
  }
}
