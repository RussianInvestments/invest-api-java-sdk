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

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Фабрика для создания обёрток над синхроными и асинхронными gRPC стабами
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
   *
   * @param stubConstructor Фабричный метод сгенерированного сервиса для создания синхронного (блокирующего) стаба
   * @return Синхронная обёртка над gRPC стабом
   */
  public <S extends AbstractBlockingStub<S>> SyncStubWrapper<S> newSyncService(Function<Channel, S> stubConstructor) {
    return new SyncStubWrapper<>(createStub(stubConstructor));
  }

  /**
   * Возвращает обёртку над асинхронным gRPC стабом сервиса
   *
   * @param stubConstructor Фабричный метод сгенерированного сервиса для создания асинхронного стаба
   * @return Асинхронная обёртка над gRPC стабом
   */
  public <S extends AbstractAsyncStub<S>> AsyncStubWrapper<S> newAsyncService(Function<Channel, S> stubConstructor) {
    return new AsyncStubWrapper<>(createStub(stubConstructor));
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
    return NettyChannelBuilder
      .forTarget(configuration.getTargetUrl())
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
