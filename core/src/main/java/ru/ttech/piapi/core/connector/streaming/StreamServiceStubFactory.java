package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Channel;
import io.grpc.stub.AbstractAsyncStub;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.internal.LoggingDebugInterceptor;

import java.util.function.Function;

/**
 * Фабрика для создания обёрток над server-side и bidirectional стримами
 * <p>Для использования требуется фабрика унарных обёрток</p>
 */
public class StreamServiceStubFactory {
  private final ServiceStubFactory serviceStubFactory;

  private StreamServiceStubFactory(ServiceStubFactory serviceStubFactory) {
    this.serviceStubFactory = serviceStubFactory;
  }

  /**
   * Метод для создания объекта фабрики
   *
   * @param serviceStubFactory Фабрика унарных обёрток
   *                           <p>Требуется для переиспользования канала и
   *                           получения некоторых параметров конфигурации</p>
   * @return Фабрика для создания обёрток над стримами
   */
  public static StreamServiceStubFactory create(ServiceStubFactory serviceStubFactory) {
    return new StreamServiceStubFactory(serviceStubFactory);
  }

  /**
   * Метод для создания обёрток над server-side стримами
   *
   * @param configuration Конфигурации для создания обёртки
   * @return Обёртка над server-side стримом
   */
  public <ReqT, RespT, S extends AbstractAsyncStub<S>> ServerSideStreamWrapper<S, RespT> newServerSideStream(
    ServerSideStreamConfiguration<S, ReqT, RespT> configuration
  ) {
    var stub = createStub(configuration.getStubConstructor());
    return new ServerSideStreamWrapper<>(
      stub, configuration.getMethod(), configuration.getCall(), configuration.getResponseObserver()
    );
  }

  /**
   * Метод для создания обёрток над bidirectional стримами
   *
   * @param configuration Конфигурации для создания обёртки
   * @return Обёртка над bidirectional стримом
   */
  public <ReqT, RespT, S extends AbstractAsyncStub<S>> BidirectionalStreamWrapper<S, ReqT, RespT> newBidirectionalStream(
    BidirectionalStreamConfiguration<S, ReqT, RespT> configuration
  ) {
    var stub = createStub(configuration.getStubConstructor());
    return new BidirectionalStreamWrapper<>(
      stub, configuration.getMethod(), configuration.getCall(), configuration.getResponseObserver()
    );
  }

  private <S extends AbstractAsyncStub<S>> S createStub(Function<Channel, S> stubConstructor) {
    var stub = stubConstructor.apply(serviceStubFactory.getChannel());
    if (serviceStubFactory.getConfiguration().isGrpcDebug()) {
      stub = stub.withInterceptors(new LoggingDebugInterceptor());
    }
    return stub;
  }
}
