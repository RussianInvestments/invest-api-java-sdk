package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Channel;
import io.grpc.stub.AbstractAsyncStub;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.internal.LoggingDebugInterceptor;
import ru.ttech.piapi.core.connector.resilience.ResilienceServerSideStreamWrapper;
import ru.ttech.piapi.core.connector.resilience.ResilienceServerSideStreamWrapperConfiguration;
import ru.ttech.piapi.core.impl.marketdata.MarketDataStreamConfiguration;
import ru.ttech.piapi.core.impl.marketdata.MarketDataStreamWrapper;
import ru.ttech.piapi.core.impl.marketdata.MarketDataStreamWrapperConfiguration;
import ru.ttech.piapi.core.impl.operations.PortfolioStreamWrapperConfiguration;
import ru.ttech.piapi.core.impl.operations.PositionsStreamWrapperConfiguration;
import ru.ttech.piapi.core.impl.orders.OrderStateStreamWrapperConfiguration;
import ru.ttech.piapi.core.impl.orders.TradeStreamWrapperConfiguration;

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
   * Метод для создания обёрток над server-side стримами ({@link ServerSideStreamWrapper})
   *
   * @param configuration Конфигурации для создания обёртки
   * @return Обёртка над server-side стримом
   */
  public <ReqT, RespT, S extends AbstractAsyncStub<S>> ServerSideStreamWrapper<S, RespT> newServerSideStream(
    ServerSideStreamConfiguration<S, ReqT, RespT> configuration
  ) {
    var stub = createStub(configuration.getStubConstructor());
    return new ServerSideStreamWrapper<>(stub, configuration);
  }

  /**
   * Метод для создания resilience-обрёток ({@link ResilienceServerSideStreamWrapper}) над врапперами
   * server-side стримов ({@link ServerSideStreamWrapper})
   *
   * @param configuration Конфигурации для создания обёртки
   *                      <p>Доступны следующие конфигурации
   *                      <ul>
   *                      <li>{@link PortfolioStreamWrapperConfiguration}</li>
   *                      <li>{@link PositionsStreamWrapperConfiguration}</li>
   *                      <li>{@link OrderStateStreamWrapperConfiguration}</li>
   *                      <li>{@link TradeStreamWrapperConfiguration}</li>
   *                      </ul>
   * @return Resilience-обрётка над server-side стримом
   */
  public <ReqT, RespT> ResilienceServerSideStreamWrapper<ReqT, RespT> newResilienceServerSideStream(
    ResilienceServerSideStreamWrapperConfiguration<ReqT, RespT> configuration
  ) {
    return new ResilienceServerSideStreamWrapper<>(this, configuration);
  }

  /**
   * Метод для создания обёрток над bidirectional стримами ({@link BidirectionalStreamWrapper})
   *
   * @param configuration Конфигурация для создания обёртки.
   *                      <p>Доступны следующие конфигурации
   *                      <ul>
   *                      <li>{@link BidirectionalStreamConfiguration}</li>
   *                      <li>{@link MarketDataStreamConfiguration}</li>
   *                      </ul>
   * @return Обёртка над bidirectional стримом
   */
  public <ReqT, RespT, S extends AbstractAsyncStub<S>> BidirectionalStreamWrapper<S, ReqT, RespT> newBidirectionalStream(
    BidirectionalStreamConfiguration<S, ReqT, RespT> configuration
  ) {
    var stub = createStub(configuration.getStubConstructor());
    return new BidirectionalStreamWrapper<>(stub, configuration);
  }

  /**
   * Метод для создания resilience-обрётки над {@link BidirectionalStreamWrapper} для MarketDataStream
   *
   * @param configuration Конфигурация для создания обёртки
   * @return Resilience-обрётка над bidirectional стримом
   */
  public MarketDataStreamWrapper newResilienceMarketDataStream(MarketDataStreamWrapperConfiguration configuration) {
    return new MarketDataStreamWrapper(this, configuration);
  }

  public ServiceStubFactory getServiceStubFactory() {
    return serviceStubFactory;
  }

  private <S extends AbstractAsyncStub<S>> S createStub(Function<Channel, S> stubConstructor) {
    var stub = stubConstructor.apply(serviceStubFactory.getChannel());
    if (serviceStubFactory.getConfiguration().isGrpcDebug()) {
      stub = stub.withInterceptors(new LoggingDebugInterceptor());
    }
    return stub;
  }
}
