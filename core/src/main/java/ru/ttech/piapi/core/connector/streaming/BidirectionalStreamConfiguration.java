package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Конфигурация обёртки над bidirectional стримом
 */
public class BidirectionalStreamConfiguration<S extends AbstractAsyncStub<S>, ReqT, RespT>
  extends BaseStreamConfiguration<S, ReqT, RespT> {

  private final BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> call;

  private BidirectionalStreamConfiguration(
    Function<Channel, S> stubConstructor,
    MethodDescriptor<ReqT, RespT> method,
    BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> call,
    StreamResponseObserver<RespT> responseObserver
  ) {
    super(stubConstructor, method, responseObserver);
    this.call = call;
  }

  BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> getCall() {
    return call;
  }

  /**
   * Метод создания билдера для создания конфигурации обёртки bidirectional стрима
   *
   * @param stubConstructor Сгенерированный конструктор gRPC стаба
   *                        <p>Пример: <code>MarketDataStreamServiceGrpc::newStub</code></p>
   * @param method Метод сервиса, к которому будет подключен стрим
   *               <p>Пример: <code>MarketDataStreamServiceGrpc.getMarketDataStreamMethod()</code></p>
   * @param call Вызов указанного метода сервиса с переданным запросом.
   *             <p>Пример: <code>MarketDataStreamServiceGrpc.MarketDataStreamServiceStub::marketDataStream</code></p>
   * @return Объект билдера конфигурации обёртки стрима
   */
  public static <S extends AbstractAsyncStub<S>, ReqT, RespT> Builder<S, ReqT, RespT> builder(
    Function<Channel, S> stubConstructor,
    MethodDescriptor<ReqT, RespT> method,
    BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> call
  ) {
    return new Builder<>(stubConstructor, method, call);
  }

  public static class Builder<S extends AbstractAsyncStub<S>, ReqT, RespT>
    extends BaseBuilder<S, ReqT, RespT, Builder<S, ReqT, RespT>> {

    private final BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> call;

    private Builder(
      Function<Channel, S> stubConstructor,
      MethodDescriptor<ReqT, RespT> method,
      BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> call
    ) {
      super(stubConstructor, method);
      this.call = call;
    }

    /**
     * Метод для создания конфигурации обёртки bidirectional стрима
     * @return Конфигурация обёртки bidirectional стрима
     */
    public BidirectionalStreamConfiguration<S, ReqT, RespT> build() {
      return new BidirectionalStreamConfiguration<>(stubConstructor, method, call, createResponseObserver());
    }
  }
}
