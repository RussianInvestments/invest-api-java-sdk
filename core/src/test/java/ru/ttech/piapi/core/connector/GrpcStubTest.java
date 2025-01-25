package ru.ttech.piapi.core.connector;

import io.github.resilience4j.retry.RetryConfig;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.vavr.Tuple;
import io.vavr.Tuple2;
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
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.contract.v1.MarketDataServiceGrpc;
import ru.tinkoff.piapi.contract.v1.MarketDataStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamRequest;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamResponse;
import ru.tinkoff.piapi.contract.v1.OrdersStreamServiceGrpc;
import ru.ttech.piapi.core.connector.resilience.ResilienceConfiguration;
import ru.ttech.piapi.core.connector.streaming.BidirectionalStreamConfiguration;
import ru.ttech.piapi.core.connector.streaming.ServerSideStreamConfiguration;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.grpcmock.GrpcMock.bidiStreamingMethod;
import static org.grpcmock.GrpcMock.calledMethod;
import static org.grpcmock.GrpcMock.response;
import static org.grpcmock.GrpcMock.serverStreamingMethod;
import static org.grpcmock.GrpcMock.statusException;
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
  public void syncRetry_success() {
    var request = GetLastPricesRequest.getDefaultInstance();
    stubFor(unaryMethod(MarketDataServiceGrpc.getGetLastPricesMethod())
      .withRequest(request)
      .willReturn(statusException(Status.UNAVAILABLE))
      .nextWillReturn(statusException(Status.UNAVAILABLE))
      .nextWillReturn(statusException(Status.UNAVAILABLE))
      .nextWillReturn(GetLastPricesResponse.getDefaultInstance()));

    var factoryWithConfig = createStubFactory();
    var factory = factoryWithConfig._1();
    var configuration = factoryWithConfig._2();
    var executorService = Executors.newSingleThreadScheduledExecutor();
    var resilienceConfiguration = ResilienceConfiguration.builder(executorService, configuration)
      .addServiceRetryConfig(
        MarketDataServiceGrpc.getServiceDescriptor(),
        RetryConfig.custom().waitDuration(Duration.ofMillis(100)).maxAttempts(5).build())
      .build();

    // создаём сервис, защищённый retry
    var resilienceSyncService = factory.newResilienceSyncService(
      MarketDataServiceGrpc::newBlockingStub,
      resilienceConfiguration
    );
    var syncResponse = resilienceSyncService.callSyncMethod(
      MarketDataServiceGrpc.getGetLastPricesMethod(),
      stub -> stub.getLastPrices(request)
    );

    assertThat(syncResponse).isEqualTo(GetLastPricesResponse.getDefaultInstance());
    verifyThat(
      calledMethod(MarketDataServiceGrpc.getGetLastPricesMethod())
        .withRequest(request),
      times(4)
    );
  }

  @Test
  public void syncRetry_failByWrongStatus() {
    var request = GetLastPricesRequest.getDefaultInstance();
    stubFor(unaryMethod(MarketDataServiceGrpc.getGetLastPricesMethod())
      .withRequest(request)
      .willReturn(statusException(Status.UNAVAILABLE)));

    var factoryWithConfig = createStubFactory();
    var factory = factoryWithConfig._1();
    var configuration = factoryWithConfig._2();
    var executorService = Executors.newSingleThreadScheduledExecutor();
    var resilienceConfiguration = ResilienceConfiguration.builder(executorService, configuration)
      .addMethodRetryConfig(
        MarketDataServiceGrpc.getGetLastPricesMethod(),
        RetryConfig.custom().waitDuration(Duration.ofMillis(100)).maxAttempts(5).build())
      .build();

    // создаём сервис, защищённый retry
    var resilienceSyncService = factory.newResilienceSyncService(
      MarketDataServiceGrpc::newBlockingStub,
      resilienceConfiguration
    );

    assertThatThrownBy(() -> resilienceSyncService.callSyncMethod(
      MarketDataServiceGrpc.getGetLastPricesMethod(),
      stub -> stub.getLastPrices(request)
    ));
    verifyThat(
      calledMethod(MarketDataServiceGrpc.getGetLastPricesMethod())
        .withRequest(request),
      times(5)
    );
  }

  @Test
  public void asyncRetry_success() {
    var request = GetLastPricesRequest.getDefaultInstance();
    stubFor(unaryMethod(MarketDataServiceGrpc.getGetLastPricesMethod())
      .withRequest(request)
      .willReturn(statusException(Status.UNAVAILABLE))
      .nextWillReturn(statusException(Status.UNAVAILABLE))
      .nextWillReturn(GetLastPricesResponse.getDefaultInstance()));

    var factoryWithConfig = createStubFactory();
    var factory = factoryWithConfig._1();
    var configuration = factoryWithConfig._2();
    var executorService = Executors.newSingleThreadScheduledExecutor();
    var resilienceConfiguration = ResilienceConfiguration.builder(executorService, configuration).build();

    // создаём сервис, защищённый retry
    var resilienceAsyncService = factory.newResilienceAsyncService(
      MarketDataServiceGrpc::newStub,
      resilienceConfiguration
    );

    CompletableFuture<GetLastPricesResponse> asyncResponse =
      resilienceAsyncService.callAsyncMethod(
        MarketDataServiceGrpc.getGetLastPricesMethod(),
        (stub, observer) -> stub.getLastPrices(request, observer)
      );

    assertThat(asyncResponse.join()).isEqualTo(GetLastPricesResponse.getDefaultInstance());
    verifyThat(
      calledMethod(MarketDataServiceGrpc.getGetLastPricesMethod())
        .withRequest(request),
      times(3)
    );
  }

  @Test
  public void asyncRetry_failByWrongStatus() {
    var request = GetLastPricesRequest.getDefaultInstance();
    stubFor(unaryMethod(MarketDataServiceGrpc.getGetLastPricesMethod())
      .withRequest(request)
      .willReturn(statusException(Status.UNAVAILABLE)));

    var factoryWithConfig = createStubFactory();
    var factory = factoryWithConfig._1();
    var configuration = factoryWithConfig._2();
    var executorService = Executors.newSingleThreadScheduledExecutor();
    var resilienceConfiguration = ResilienceConfiguration.builder(executorService, configuration)
      // создаём две конфигурации, на сервис и на метод. Конфигурация на метод считается приоритетной
      .addMethodRetryConfig(
        MarketDataServiceGrpc.getGetLastPricesMethod(),
        RetryConfig.custom().waitDuration(Duration.ofMillis(100)).maxAttempts(5).build())
      .addServiceRetryConfig(
        MarketDataServiceGrpc.getServiceDescriptor(),
        RetryConfig.custom().waitDuration(Duration.ofMillis(100)).maxAttempts(3).build())
      .build();

    // создаём сервис, защищённый retry
    var resilienceAsyncService = factory.newResilienceAsyncService(
      MarketDataServiceGrpc::newStub,
      resilienceConfiguration
    );

    assertThatThrownBy(() -> {
      CompletableFuture<GetLastPricesResponse> asyncResponse =
        resilienceAsyncService.callAsyncMethod(
          MarketDataServiceGrpc.getGetLastPricesMethod(),
          (stub, observer) -> stub.getLastPrices(request, observer)
        );
      asyncResponse.join();
    });
    verifyThat(
      calledMethod(MarketDataServiceGrpc.getGetLastPricesMethod())
        .withRequest(request),
      times(5)
    );
  }

  @Test
  public void contextFork_success() {
    // arrange stub and data
    var request = GetLastPricesRequest.getDefaultInstance();
    var priceResponse = GetLastPricesResponse.getDefaultInstance();
    stubFor(unaryMethod(MarketDataServiceGrpc.getGetLastPricesMethod())
      .withRequest(request)
      .willReturn(statusException(Status.CANCELLED).withFixedDelay(200))
      .nextWillReturn(GrpcMock.response(priceResponse)));

    // async stub example
    var factoryWithConfig = createStubFactory();
    var factory = factoryWithConfig._1();
    var asyncService = factory.newAsyncService(MarketDataServiceGrpc::newStub);
    CompletableFuture<GetLastPricesResponse> asyncResponseOne =
      asyncService.callAsyncMethod((stub, observer) -> stub.getLastPrices(request, observer));

    // ловим cancelled
    assertThatThrownBy(asyncResponseOne::join);

    // отправляем ещё два запроса
    CompletableFuture<GetLastPricesResponse> asyncResponseTwo =
      asyncService.callAsyncMethod((stub, observer) -> stub.getLastPrices(request, observer));
    CompletableFuture<GetLastPricesResponse> asyncResponseThree =
      asyncService.callAsyncMethod((stub, observer) -> stub.getLastPrices(request, observer));

    // и проверяем, что всё выполнено успешно
    assertThat(asyncResponseTwo.join()).isEqualTo(priceResponse);
    assertThat(asyncResponseThree.join()).isEqualTo(priceResponse);
    verifyThat(
      calledMethod(MarketDataServiceGrpc.getGetLastPricesMethod())
        .withRequest(request),
      times(3)
    );
  }

  @SneakyThrows
  @Test
  public void serverSideStream_success() {
    // setup mock stub
    var response = OrderStateStreamResponse.getDefaultInstance();
    stubFor(serverStreamingMethod(OrdersStreamServiceGrpc.getOrderStateStreamMethod())
      .willReturn(
        stream(response(response).withFixedDelay(500))
          .and(response(response).withFixedDelay(500))
          .and(response(response).withFixedDelay(500))
      ));

    // setup server-side stream
    var factoryWithConfig = createStubFactory();
    var factory = factoryWithConfig._1();
    var streamFactory = StreamServiceStubFactory.create(factory);
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
  public void bidirectionalStream_success() {
    // setup mock stub
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

    // setup bidirectional stream
    var factoryWithConfig = createStubFactory();
    var factory = factoryWithConfig._1();
    var streamFactory = StreamServiceStubFactory.create(factory);
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

  private Tuple2<ServiceStubFactory, ConnectorConfiguration> createStubFactory() {
    var properties = loadPropertiesFromFile("invest.properties");
    var configuration = ConnectorConfiguration.loadFromProperties(properties);
    return Tuple.of(ServiceStubFactory.create(configuration, () -> channel), configuration);
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
