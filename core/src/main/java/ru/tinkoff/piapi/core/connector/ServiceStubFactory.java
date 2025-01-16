package ru.tinkoff.piapi.core.connector;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.MetadataUtils;
import ru.tinkoff.piapi.core.connector.internal.LoggingDebugInterceptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ServiceStubFactory {

  private final ConnectorConfiguration configuration;

  private ServiceStubFactory(ConnectorConfiguration configuration) {
    this.configuration = configuration;
  }

  public <S extends AbstractBlockingStub<S>> SyncStubWrapper<S> newSyncService(Function<Channel, S> constructor) {
    var channel = createChannel();
    var stub = constructor.apply(channel).withInterceptors();
    return new SyncStubWrapper<>(stub);
  }

  public <S extends AbstractBlockingStub<S>> SyncStubWrapper<S> newSyncService(Function<Channel, S> constructor, ManagedChannel channel) {
    var stub = constructor.apply(channel).withInterceptors();
    return new SyncStubWrapper<>(stub);
  }

  public <S extends AbstractAsyncStub<S>> AsyncStubWrapper<S> newAsyncService(Function<Channel, S> constructor) {
    var channel = createChannel();
    var stub = constructor.apply(channel).withInterceptors();
    return new AsyncStubWrapper<>(stub);
  }

  public <S extends AbstractAsyncStub<S>> AsyncStubWrapper<S> newAsyncService(Function<Channel, S> constructor, ManagedChannel channel) {
    var stub = constructor.apply(channel).withInterceptors();
    return new AsyncStubWrapper<>(stub);
  }

  public static ServiceStubFactory create(ConnectorConfiguration configuration) {
    return new ServiceStubFactory(configuration);
  }

  private Channel createChannel() {
    var headers = new Metadata();
    addAuthHeader(headers, configuration.getToken());
    addAppNameHeader(headers, configuration.getAppName());
    return NettyChannelBuilder
      .forTarget(configuration.getTargetUrl())
      .intercept(
        new LoggingDebugInterceptor(),
        MetadataUtils.newAttachHeadersInterceptor(headers)
      )
      .withOption(
        ChannelOption.CONNECT_TIMEOUT_MILLIS, // TODO: заменить на значение из конфига
        (int) Duration.ofSeconds(30).toMillis()
      )
      .keepAliveTimeout(60, TimeUnit.SECONDS) // TODO: заменить на значение из конфига
      .maxInboundMessageSize(16777216) // 16 Mb // TODO: заменить на значение из конфига
      .useTransportSecurity()
      .build();
  }

  private void addAuthHeader(Metadata metadata, String token) {
    var authKey = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    metadata.put(authKey, "Bearer " + token);
  }

  private void addAppNameHeader(Metadata metadata, String appName) {
    var key = Metadata.Key.of("x-app-name", Metadata.ASCII_STRING_MARSHALLER);
    metadata.put(key, appName);
  }
}
