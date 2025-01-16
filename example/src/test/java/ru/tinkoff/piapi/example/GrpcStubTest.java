package ru.tinkoff.piapi.example;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.grpcmock.GrpcMock;
import org.grpcmock.junit5.GrpcMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import ru.tinkoff.piapi.contract.v1.GetLastPricesRequest;
import ru.tinkoff.piapi.contract.v1.GetLastPricesResponse;
import ru.tinkoff.piapi.contract.v1.LastPriceType;
import ru.tinkoff.piapi.contract.v1.MarketDataServiceGrpc;
import ru.tinkoff.piapi.core.connector.ConnectorConfiguration;
import ru.tinkoff.piapi.core.connector.ServiceStubFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.grpcmock.GrpcMock.*;

public class GrpcStubTest {

  @RegisterExtension
  static GrpcMockExtension grpcMockExtension = GrpcMockExtension.builder()
    .build();
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

  @Test
  public void test() {
    // arrange stub and data
    var request = GetLastPricesRequest.newBuilder()
      .addInstrumentId("8e2b0325-0292-4654-8a18-4f63ed3b0e09") // UID акции Банк ВТБ
      .setLastPriceType(LastPriceType.LAST_PRICE_DEALER)
      .build();
    var response = GetLastPricesResponse.getDefaultInstance();
    stubFor(unaryMethod(MarketDataServiceGrpc.getGetLastPricesMethod())
      .willReturn(GrpcMock.response(response)));

    // setup client
    var properties = loadPropertiesFromFile("invest.properties");
    properties.setProperty("target", String.format("localhost:%d", GrpcMock.getGlobalPort()));
    var configuration = ConnectorConfiguration.loadFromProperties(properties);
    var factory = ServiceStubFactory.create(configuration);

    // sync stub example
    var syncService = factory.newSyncService(MarketDataServiceGrpc::newBlockingStub, channel);
    GetLastPricesResponse syncResponse = syncService.callSyncMethod(stub -> stub.getLastPrices(request));

    // async stub example
    var asyncService = factory.newAsyncService(MarketDataServiceGrpc::newStub, channel);
    CompletableFuture<GetLastPricesResponse> asyncResponse =
      asyncService.callAsyncMethod((stub, observer) -> stub.getLastPrices(request, observer));

    assertThat(syncResponse).isEqualTo(response);
    assertThat(asyncResponse.join()).isEqualTo(response);
    verifyThat(
      calledMethod(MarketDataServiceGrpc.getGetLastPricesMethod())
        .withRequest(request),
      times(2)
    );
  }

  private static Properties loadPropertiesFromFile(String filename) {
    Properties prop = new Properties();
    try (InputStream input = GrpcStubTest.class.getClassLoader().getResourceAsStream(filename)) {
      if (input == null) {
        throw new IllegalArgumentException("Could not load properties file!");
      }
      prop.load(input);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return prop;
  }
}
