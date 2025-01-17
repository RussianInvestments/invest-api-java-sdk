package ru.ttech.piapi.core.connector.internal;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
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
    if (!logger.isDebugEnabled()) {
      logger.warn("Отладка включена, но уровень логирования выше, чем debug. " +
        "Отключите отладку или понизьте уровень логирования");
      return next.newCall(method, callOptions);
    }
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
      logger.debug("Готовится вызов метода {} сервиса {}.", method.getBareMethodName(), method.getServiceName());
      super.start(new LoggingClientCallListener<>(responseListener, logger, method), headers);
    }
  }

  static class LoggingClientCallListener<RespT>
    extends ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT> {

    private static final Metadata.Key<String> trackingIdKey =
      Metadata.Key.of("x-tracking-id", Metadata.ASCII_STRING_MARSHALLER);

    private final Logger logger;
    private final MethodDescriptor<?, RespT> method;

    LoggingClientCallListener(ClientCall.Listener<RespT> listener, Logger logger, MethodDescriptor<?, RespT> method) {
      super(listener);
      this.logger = logger;
      this.method = method;
    }

    @Override
    public void onHeaders(Metadata headers) {
      logger.debug("Получен ответ с tracking id: {}", headers.get(trackingIdKey));
      delegate().onHeaders(headers);
    }

    @Override
    public void onMessage(RespT message) {
      if (method.getType() == MethodDescriptor.MethodType.UNARY) {
        logger.debug(
          "Пришёл ответ от метода {} сервиса {}: {}",
          method.getBareMethodName(), method.getServiceName(), message
        );
      } else {
        logger.debug(
          "Пришло сообщение от потока {} сервиса {}: {}",
          method.getBareMethodName(), method.getServiceName(), message
        );
      }
      delegate().onMessage(message);
    }

    @Override
    public void onClose(Status status, Metadata trailers) {
      logger.debug("Соединение завершилось со статусом: {} и метаданными: {}", status, trailers);
      delegate().onClose(status, trailers);
    }
  }
}
