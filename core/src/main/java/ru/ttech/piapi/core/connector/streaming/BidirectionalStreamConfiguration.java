package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Конфигурация для {@link BidirectionalStreamWrapper}
 */
public class BidirectionalStreamConfiguration<S extends AbstractAsyncStub<S>, ReqT, RespT>
  extends BaseStreamConfiguration<S, ReqT, RespT> {

  protected final BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> call;

  protected BidirectionalStreamConfiguration(
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
   * <p>Пример вызова:<pre>{@code
   * var streamConfiguration = BidirectionalStreamConfiguration.builder(
   *           MarketDataStreamServiceGrpc::newStub,
   *           MarketDataStreamServiceGrpc.getMarketDataStreamMethod(),
   *           MarketDataStreamServiceGrpc.MarketDataStreamServiceStub::marketDataStream)
   *         .addOnNextListener(response -> logger.info("Сообщение: {}", response))
   *         .addOnErrorListener(e -> logger.error("Исключение: {}", e.getMessage()))
   *         .addOnCompleteListener(() -> logger.info("Стрим завершен"))
   *         .build()
   * }</pre>
   *
   * @param stubConstructor Сгенерированный конструктор gRPC стаба
   * @param method Метод сервиса, к которому будет подключен стрим
   * @param call Вызов указанного метода сервиса с переданным запросом.
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

    protected final BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> call;

    protected Builder(
      Function<Channel, S> stubConstructor,
      MethodDescriptor<ReqT, RespT> method,
      BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> call
    ) {
      super(stubConstructor, method);
      this.call = call;
    }

    /**
     * Метод для создания конфигурации для {@link BidirectionalStreamWrapper}
     *
     * @return Конфигурация для {@link BidirectionalStreamWrapper}
     */
    public BidirectionalStreamConfiguration<S, ReqT, RespT> build() {
      return new BidirectionalStreamConfiguration<>(stubConstructor, method, call, createResponseObserver());
    }
  }
}
