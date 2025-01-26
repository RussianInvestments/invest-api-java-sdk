package ru.ttech.piapi.core.connector.unary;

import io.github.resilience4j.retry.RetryConfig;
import io.grpc.Status;
import org.grpcmock.junit5.GrpcMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.tinkoff.piapi.contract.v1.GetLastPricesRequest;
import ru.tinkoff.piapi.contract.v1.GetLastPricesResponse;
import ru.tinkoff.piapi.contract.v1.MarketDataServiceGrpc;
import ru.ttech.piapi.core.connector.GrpcStubBaseTest;
import ru.ttech.piapi.core.connector.resilience.ResilienceConfiguration;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.grpcmock.GrpcMock.calledMethod;
import static org.grpcmock.GrpcMock.statusException;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.times;
import static org.grpcmock.GrpcMock.unaryMethod;
import static org.grpcmock.GrpcMock.verifyThat;

@ExtendWith(GrpcMockExtension.class)
public class GrpcUnaryStubResilienceTest extends GrpcStubBaseTest {

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
      .withRetryForService(
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
      .withRetryForMethod(
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
      .withRetryForMethod(
        MarketDataServiceGrpc.getGetLastPricesMethod(),
        RetryConfig.custom().waitDuration(Duration.ofMillis(100)).maxAttempts(5).build())
      .withRetryForService(
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
}
