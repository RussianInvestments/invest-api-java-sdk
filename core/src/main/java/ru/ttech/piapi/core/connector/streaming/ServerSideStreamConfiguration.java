package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;
import ru.ttech.piapi.core.connector.streaming.listeners.OnCompleteListener;
import ru.ttech.piapi.core.connector.streaming.listeners.OnErrorListener;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Конфигурация для {@link ServerSideStreamWrapper}
 */
public class ServerSideStreamConfiguration<S extends AbstractAsyncStub<S>, ReqT, RespT>
  extends BaseStreamConfiguration<S, ReqT, RespT> {

  private final BiConsumer<S, StreamObserver<RespT>> call;

  private ServerSideStreamConfiguration(
    Function<Channel, S> stubConstructor,
    MethodDescriptor<ReqT, RespT> method,
    BiConsumer<S, StreamObserver<RespT>> call,
    List<OnNextListener<RespT>> onNextListeners,
    List<OnErrorListener> onErrorListeners,
    List<OnCompleteListener> onCompleteListeners
  ) {
    super(stubConstructor, method, onNextListeners, onErrorListeners, onCompleteListeners);
    this.call = call;
  }

  protected BiConsumer<S, StreamObserver<RespT>> getCall() {
    return call;
  }

  /**
   * Метод создания билдера для создания конфигурации обёртки server-side стрима
   * <p>Пример вызова:<pre>{@code
   * var request = OrderStateStreamRequest.getDefaultInstance();
   * var streamConfiguration = ServerSideStreamConfiguration.builder(
   *           OrdersStreamServiceGrpc::newStub,
   *           OrdersStreamServiceGrpc.getOrderStateStreamMethod(),
   *           (stub, observer) -> stub.orderStateStream(request, observer))
   *         .addOnNextListener(response -> logger.info("Сообщение: {}", response))
   *         .addOnErrorListener(e -> logger.error("Исключение: {}", e.getMessage()))
   *         .addOnCompleteListener(() -> logger.info("Стрим завершен"))
   *         .build()
   * }</pre>
   *
   * @param stubConstructor Сгенерированный конструктор gRPC стаба
   * @param method          Метод сервиса, к которому будет подключен стрим
   * @param call            Вызов указанного метода сервиса с переданным запросом.
   * @return Объект билдера конфигурации обёртки стрима
   */
  public static <S extends AbstractAsyncStub<S>, ReqT, RespT> Builder<S, ReqT, RespT> builder(
    Function<Channel, S> stubConstructor,
    MethodDescriptor<ReqT, RespT> method,
    BiConsumer<S, StreamObserver<RespT>> call
  ) {
    return new Builder<>(stubConstructor, method, call);
  }

  public static class Builder<S extends AbstractAsyncStub<S>, ReqT, RespT>
    extends BaseBuilder<S, ReqT, RespT, Builder<S, ReqT, RespT>> {

    private final BiConsumer<S, StreamObserver<RespT>> call;

    private Builder(
      Function<Channel, S> stubConstructor,
      MethodDescriptor<ReqT, RespT> method,
      BiConsumer<S, StreamObserver<RespT>> call
    ) {
      super(stubConstructor, method);
      this.call = call;
    }

    /**
     * Метод для создания конфигурации {@link ServerSideStreamWrapper}
     *
     * @return Конфигурация для {@link ServerSideStreamWrapper}
     */
    public ServerSideStreamConfiguration<S, ReqT, RespT> build() {
      return new ServerSideStreamConfiguration<>(
        stubConstructor, method, call, onNextListeners, onErrorListeners, onCompleteListeners
      );
    }
  }
}
