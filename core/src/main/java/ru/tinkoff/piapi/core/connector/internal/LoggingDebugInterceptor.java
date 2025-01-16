package ru.tinkoff.piapi.core.connector.internal;

import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoggingDebugInterceptor implements ClientInterceptor {

  private final Logger logger = LoggerFactory.getLogger(LoggingDebugInterceptor.class);

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
    MethodDescriptor<ReqT, RespT> method,
    CallOptions callOptions,
    Channel next
  ) {
    return new LoggingClientCall<>(
      next.newCall(method, callOptions), logger, method);
  }

  static class LoggingClientCall<ReqT, RespT>
    extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {

    private final Logger logger;
    private final MethodDescriptor<ReqT, RespT> method;

    LoggingClientCall(
      ClientCall<ReqT, RespT> call,
      Logger logger,
      MethodDescriptor<ReqT, RespT> method) {
      super(call);
      this.logger = logger;
      this.method = method;
    }

    @Override
    public void start(ClientCall.Listener<RespT> responseListener, Metadata headers) {
      logger.debug(
        "Готовится вызов метода {} сервиса {}.",
        method.getBareMethodName(),
        method.getServiceName());
      super.start(
        new LoggingClientCallListener<>(responseListener, logger, method),
        headers);
    }
  }

  static class LoggingClientCallListener<RespT>
    extends ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT> {

    private static final Metadata.Key<String> trackingIdKey =
      Metadata.Key.of("x-tracking-id", Metadata.ASCII_STRING_MARSHALLER);

    private final Logger logger;
    private final MethodDescriptor<?, RespT> method;
    volatile private String lastTrackingId;

    LoggingClientCallListener(
      ClientCall.Listener<RespT> listener,
      Logger logger,
      MethodDescriptor<?, RespT> method) {
      super(listener);
      this.logger = logger;
      this.method = method;
    }

    @Override
    public void onHeaders(Metadata headers) {
      lastTrackingId = headers.get(trackingIdKey);
      delegate().onHeaders(headers);
    }

    @Override
    public void onMessage(RespT message) {
      if (method.getType() == MethodDescriptor.MethodType.UNARY) {
        logger.debug(
          "Пришёл ответ от метода {} сервиса {}. (x-tracking-id = {})",
          method.getBareMethodName(),
          method.getServiceName(),
          lastTrackingId
        );
      } else {
        logger.debug(
          "Пришло сообщение от потока {} сервиса {}.",
          method.getBareMethodName(),
          method.getServiceName()
        );
      }
      delegate().onMessage(message);
    }
  }
}
