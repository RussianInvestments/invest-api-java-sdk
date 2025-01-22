package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Конфигурация обёртки над server-side стримом
 */
public class ServerSideStreamConfiguration<S extends AbstractAsyncStub<S>, ReqT, RespT>
  extends BaseStreamConfiguration<S, ReqT, RespT> {

  private final BiConsumer<S, StreamObserver<RespT>> call;

  private ServerSideStreamConfiguration(
    Function<Channel, S> stubConstructor,
    MethodDescriptor<ReqT, RespT> method,
    BiConsumer<S, StreamObserver<RespT>> call,
    StreamResponseObserver<RespT> responseObserver
  ) {
    super(stubConstructor, method, responseObserver);
    this.call = call;
  }

  BiConsumer<S, StreamObserver<RespT>> getCall() {
    return call;
  }

  /**
   * Метод создания билдера для создания конфигурации обёртки server-side стрима
   *
   * @param stubConstructor Сгенерированный конструктор gRPC стаба
   *                        <p>Пример: <code>OrdersStreamServiceGrpc::newStub</code></p>
   * @param method Метод сервиса, к которому будет подключен стрим
   *               <p>Пример: <code>OrdersStreamServiceGrpc.getOrderStateStreamMethod()</code></p>
   * @param call Вызов указанного метода сервиса с переданным запросом.
   *             <p>Для корректной работы observer нужно передать из лямбды</p>
   *             <p>Пример: <code>(stub, observer) -> stub.orderStateStream(request, observer))</code></p>
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
     * Метод для создания конфигурации обёртки server-side стрима
     *
     * @return Конфигурация обёртки server-side стрима
     */
    public ServerSideStreamConfiguration<S, ReqT, RespT> build() {
      return new ServerSideStreamConfiguration<>(stubConstructor, method, call, createResponseObserver());
    }
  }
}
