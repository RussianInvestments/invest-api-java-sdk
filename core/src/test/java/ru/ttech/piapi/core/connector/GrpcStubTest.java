package ru.ttech.piapi.core.connector;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import org.grpcmock.GrpcMock;
import org.grpcmock.junit5.GrpcMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.GetLastPricesRequest;
import ru.tinkoff.piapi.contract.v1.GetLastPricesResponse;
import ru.tinkoff.piapi.contract.v1.LastPriceType;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.contract.v1.MarketDataServiceGrpc;
import ru.tinkoff.piapi.contract.v1.MarketDataStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamRequest;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamResponse;
import ru.tinkoff.piapi.contract.v1.OrdersStreamServiceGrpc;
import ru.ttech.piapi.core.connector.streaming.BidirectionalStreamConfiguration;
import ru.ttech.piapi.core.connector.streaming.ServerSideStreamConfiguration;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.grpcmock.GrpcMock.bidiStreamingMethod;
import static org.grpcmock.GrpcMock.calledMethod;
import static org.grpcmock.GrpcMock.response;
import static org.grpcmock.GrpcMock.serverStreamingMethod;
import static org.grpcmock.GrpcMock.stream;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.times;
import static org.grpcmock.GrpcMock.unaryMethod;
import static org.grpcmock.GrpcMock.verifyThat;

public class GrpcStubTest {

  private static final Logger logger = LoggerFactory.getLogger(GrpcStubTest.class);

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
    var configuration = ConnectorConfiguration.loadFromProperties(properties);
    var factory = ServiceStubFactory.create(configuration, () -> channel);

    // sync stub example
    var syncService = factory.newSyncService(MarketDataServiceGrpc::newBlockingStub);
    GetLastPricesResponse syncResponse = syncService.callSyncMethod(stub -> stub.getLastPrices(request));

    // async stub example
    var asyncService = factory.newAsyncService(MarketDataServiceGrpc::newStub);
    CompletableFuture<GetLastPricesResponse> asyncResponse =
      asyncService.callAsyncMethod((stub, observer) -> stub.getLastPrices(request, observer));

    // check results
    assertThat(syncResponse).isEqualTo(response);
    assertThat(asyncResponse.join()).isEqualTo(response);
    verifyThat(
      calledMethod(MarketDataServiceGrpc.getGetLastPricesMethod())
        .withRequest(request),
      times(2)
    );
  }

  @SneakyThrows
  @Test
  public void test_serverSideStream() {
    // setup mock stub
    var response = OrderStateStreamResponse.getDefaultInstance();
    stubFor(serverStreamingMethod(OrdersStreamServiceGrpc.getOrderStateStreamMethod())
      .willReturn(
        stream(response(response).withFixedDelay(500))
          .and(response(response).withFixedDelay(500))
          .and(response(response).withFixedDelay(500))
      ));

    // setup client
    var properties = loadPropertiesFromFile("invest.properties");
    var configuration = ConnectorConfiguration.loadFromProperties(properties);
    var factory = ServiceStubFactory.create(configuration, () -> channel);
    var streamFactory = StreamServiceStubFactory.create(factory);

    // setup server-side stream
    var stream = streamFactory.newServerSideStream(
      ServerSideStreamConfiguration.builder(
          OrdersStreamServiceGrpc::newStub,
          OrdersStreamServiceGrpc.getOrderStateStreamMethod(),
          (stub, observer) -> stub.orderStateStream(OrderStateStreamRequest.getDefaultInstance(), observer))
        .addOnNextListener(markerDataResponse -> logger.info("Сообщение: {}", markerDataResponse))
        .addOnErrorListener(throwable -> logger.error("Произошла ошибка: {}", throwable.getMessage()))
        .addOnCompleteListener(() -> logger.info("Стрим завершен"))
        .build()
    );
    stream.connect();

    // TODO: заменить на что-то другое
    Thread.sleep(2_000);
    stream.disconnect();
  }

  @SneakyThrows
  @Test
  public void test_bidirectionalStream() {
    stubFor(bidiStreamingMethod(MarketDataStreamServiceGrpc.getMarketDataStreamMethod())
      .withFirstRequest(req -> req.equals(MarketDataRequest.getDefaultInstance()))
      .willProxyTo(responseObserver -> new StreamObserver<>() {
        @Override
        public void onNext(MarketDataRequest marketDataRequest) {
          IntStream.range(0, 5).forEach(i -> {
            responseObserver.onNext(MarketDataResponse.getDefaultInstance());
            try {
              Thread.sleep(200);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          });
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onCompleted() {
          responseObserver.onCompleted();
        }
      }));
    // setup client
    var properties = loadPropertiesFromFile("invest.properties");
    var configuration = ConnectorConfiguration.loadFromProperties(properties);
    var factory = ServiceStubFactory.create(configuration, () -> channel);
    var streamFactory = StreamServiceStubFactory.create(factory);

    // setup bidirectional stream
    var stream = streamFactory.newBidirectionalStream(
      BidirectionalStreamConfiguration.builder(
          MarketDataStreamServiceGrpc::newStub,
          MarketDataStreamServiceGrpc.getMarketDataStreamMethod(),
          MarketDataStreamServiceGrpc.MarketDataStreamServiceStub::marketDataStream)
        .addOnNextListener(markerDataResponse -> logger.info("Сообщение: {}", markerDataResponse))
        .addOnErrorListener(throwable -> logger.error("Произошла ошибка: {}", throwable.getMessage()))
        .addOnCompleteListener(() -> logger.info("Стрим завершен"))
        .build()
    );
    stream.connect();
    stream.newCall(MarketDataRequest.getDefaultInstance());

    // TODO: заменить на что-то другое
    Thread.sleep(2_000);
    stream.disconnect();
  }

  private static Properties loadPropertiesFromFile(String filename) {
    Properties prop = new Properties();
    try (InputStream input = GrpcStubTest.class.getClassLoader().getResourceAsStream(filename)) {
      if (input == null) {
        throw new IllegalArgumentException("Невозможно загрузить файл настроек!");
      }
      prop.load(input);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return prop;
  }
}
