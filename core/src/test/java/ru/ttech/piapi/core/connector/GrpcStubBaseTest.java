package ru.ttech.piapi.core.connector;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.grpcmock.GrpcMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;

public abstract class GrpcStubBaseTest {
  private ManagedChannel channel;

  @BeforeEach
  void setup() {
    channel = NettyChannelBuilder.forTarget("localhost:" + GrpcMock.getGlobalPort())
      .usePlaintext()
      .build();
  }

  @AfterEach
  void cleanup() {
    Optional.ofNullable(channel).ifPresent(ManagedChannel::shutdownNow);
  }

  protected Tuple2<ServiceStubFactory, ConnectorConfiguration> createStubFactory() {
    var configuration = ConnectorConfiguration.loadPropertiesFromResources("invest.properties");
    return Tuple.of(ServiceStubFactory.create(configuration, () -> channel), configuration);
  }
}
