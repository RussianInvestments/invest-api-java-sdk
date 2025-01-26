package ru.ttech.piapi.core.connector;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;
import io.vavr.Lazy;
import ru.ttech.piapi.core.connector.internal.LoggingDebugInterceptor;
import ru.ttech.piapi.core.connector.resilience.ResilienceAsyncStubWrapper;
import ru.ttech.piapi.core.connector.resilience.ResilienceConfiguration;
import ru.ttech.piapi.core.connector.resilience.ResilienceSyncStubWrapper;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Фабрика для создания обёрток над унарными синхроными и асинхронными gRPC стабами
 * <p>Задаёт параметры для подключения gRPC стабов к API согласно переданной конфигурации
 */
public class ServiceStubFactory {

  private final ConnectorConfiguration configuration;
  private final Supplier<ManagedChannel> supplier;

  private ServiceStubFactory(ConnectorConfiguration configuration, Supplier<ManagedChannel> supplier) {
    this.configuration = configuration;
    this.supplier = supplier;
  }

  /**
   * Возвращает обёртку над синхронным gRPC стабом сервиса
   * <p>Пример использования:<pre>{@code
   * var syncService = factory.newSyncService(MarketDataServiceGrpc::newBlockingStub)
   * }</pre>
   *
   * @param stubConstructor Фабричный метод сгенерированного сервиса для создания синхронного (блокирующего) стаба
   * @return Синхронная обёртка над gRPC стабом
   */
  public <S extends AbstractBlockingStub<S>> SyncStubWrapper<S> newSyncService(Function<Channel, S> stubConstructor) {
    return new SyncStubWrapper<>(createStub(stubConstructor));
  }

  /**
   * Метод для создания обёртки над {@link SyncStubWrapper} с поддержкой resilience
   * <p>Пример использования:<pre>{@code
   *    var resilienceSyncService = factory.newResilienceSyncService(
   *       MarketDataServiceGrpc::newBlockingStub,
   *       resilienceConfiguration
   *     );
   * }</pre>
   *
   * @param stubConstructor Фабричный метод сгенерированного сервиса для создания синхронного стаба
   * @param resilienceConfiguration Конфигурация resilience для обёртки
   * @return Обёртка над {@link SyncStubWrapper}
   */
  public <S extends AbstractBlockingStub<S>> ResilienceSyncStubWrapper<S> newResilienceSyncService(
    Function<Channel, S> stubConstructor,
    ResilienceConfiguration resilienceConfiguration
  ) {
    return new ResilienceSyncStubWrapper<>(newSyncService(stubConstructor), resilienceConfiguration);
  }

  /**
   * Возвращает обёртку над асинхронным gRPC стабом сервиса
   * <p>Пример использования:<pre>{@code
   * var asyncService = factory.newAsyncService(MarketDataServiceGrpc::newStub)
   * }</pre>
   * @param stubConstructor Фабричный метод сгенерированного сервиса для создания асинхронного стаба
   * @return Асинхронная обёртка над gRPC стабом
   */
  public <S extends AbstractAsyncStub<S>> AsyncStubWrapper<S> newAsyncService(Function<Channel, S> stubConstructor) {
    return new AsyncStubWrapper<>(createStub(stubConstructor), configuration.isGrpcContextFork());
  }

  /**
   * Метод для создания обёртки над {@link AsyncStubWrapper} с поддержкой resilience
   * <p>Пример использования:<pre>{@code
   *    var resilienceAsyncService = factory.newResilienceAsyncService(
   *       MarketDataServiceGrpc::newStub,
   *       resilienceConfiguration
   *     );
   * }</pre>
   *
   * @param stubConstructor Фабричный метод сгенерированного сервиса для создания асинхронного стаба
   * @param resilienceConfiguration Конфигурация resilience для обёртки
   * @return Обёртка над {@link AsyncStubWrapper}
   */
  public <S extends AbstractAsyncStub<S>> ResilienceAsyncStubWrapper<S> newResilienceAsyncService(
    Function<Channel, S> stubConstructor,
    ResilienceConfiguration resilienceConfiguration
  ) {
    return new ResilienceAsyncStubWrapper<>(newAsyncService(stubConstructor), resilienceConfiguration);
  }

  /**
   * Создааёт фабрику с параметрами подключения согласно конфигурации
   *
   * @param configuration Конфигурация библиотеки
   * @return Фабрика для создания обёрток над стабами
   */
  public static ServiceStubFactory create(ConnectorConfiguration configuration) {
    return create(configuration, Lazy.of(() -> createChannel(configuration)));
  }

  public ManagedChannel getChannel() {
    return supplier.get();
  }

  public ConnectorConfiguration getConfiguration() {
    return configuration;
  }

  private <S extends AbstractStub<S>> S createStub(Function<Channel, S> stubConstructor) {
    var stub = stubConstructor.apply(supplier.get());
    if (configuration.isGrpcDebug()) {
      stub = stub.withInterceptors(new LoggingDebugInterceptor());
    }
    return stub;
  }

  static ServiceStubFactory create(ConnectorConfiguration configuration, Supplier<ManagedChannel> supplier) {
    return new ServiceStubFactory(configuration, supplier);
  }

  private static ManagedChannel createChannel(ConnectorConfiguration configuration) {
    var headers = new Metadata();
    addAuthHeader(headers, configuration.getToken());
    addAppNameHeader(headers, configuration.getAppName());
    String targetUrl = configuration.isSandboxEnabled()
      ? configuration.getSandboxTargetUrl()
      : configuration.getTargetUrl();
    return NettyChannelBuilder
      .forTarget(targetUrl)
      .intercept(MetadataUtils.newAttachHeadersInterceptor(headers))
      .withOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, configuration.getTimeout())
      .keepAliveTimeout(configuration.getKeepalive(), TimeUnit.MILLISECONDS)
      .maxInboundMessageSize(configuration.getMaxInboundMessageSize())
      .useTransportSecurity()
      .build();
  }

  private static void addAuthHeader(Metadata metadata, String token) {
    var authKey = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    metadata.put(authKey, "Bearer " + token);
  }

  private static void addAppNameHeader(Metadata metadata, String appName) {
    var key = Metadata.Key.of("x-app-name", Metadata.ASCII_STRING_MARSHALLER);
    metadata.put(key, appName);
  }
}
