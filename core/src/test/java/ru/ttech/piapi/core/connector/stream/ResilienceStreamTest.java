package ru.ttech.piapi.core.connector.stream;

import io.grpc.Status;
import lombok.SneakyThrows;
import org.grpcmock.junit5.GrpcMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamRequest;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamResponse;
import ru.tinkoff.piapi.contract.v1.OrdersStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.ResultSubscriptionStatus;
import ru.tinkoff.piapi.contract.v1.SubscriptionResponse;
import ru.ttech.piapi.core.connector.GrpcStubBaseTest;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.impl.orders.OrderStateStreamWrapperConfiguration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.grpcmock.GrpcMock.response;
import static org.grpcmock.GrpcMock.serverStreamingMethod;
import static org.grpcmock.GrpcMock.statusException;
import static org.grpcmock.GrpcMock.stream;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.times;
import static org.grpcmock.GrpcMock.verifyThat;

@ExtendWith(GrpcMockExtension.class)
public class ResilienceStreamTest extends GrpcStubBaseTest {

  private static final Logger logger = LoggerFactory.getLogger(ResilienceStreamTest.class);

  @SneakyThrows
  @Test
  void testServerSideStreamReconnectByError() {
    var receivedUpdates = new AtomicInteger();
    var latch = new CountDownLatch(3);
    var response = OrderStateStreamResponse.newBuilder()
      .setOrderState(OrderStateStreamResponse.OrderState.getDefaultInstance())
      .build();
    var successSubscriptionResponse = OrderStateStreamResponse.newBuilder()
      .setSubscription(SubscriptionResponse.newBuilder()
        .setStatus(ResultSubscriptionStatus.RESULT_SUBSCRIPTION_STATUS_OK)
        .build())
      .build();
    stubFor(serverStreamingMethod(OrdersStreamServiceGrpc.getOrderStateStreamMethod())
      .willReturn(
        stream(response(successSubscriptionResponse).withFixedDelay(50))
          .and(response(response).withFixedDelay(50))
          .and(response(response).withFixedDelay(50))
          .and(statusException(Status.INTERNAL)) // выкидываем статус с ошибкой
      )
    );
    var factoryWithConfig = createStubFactory();
    var factory = factoryWithConfig._1();
    var streamFactory = StreamServiceStubFactory.create(factory);
    var executorService = Executors.newSingleThreadScheduledExecutor();
    var wrapper = streamFactory.newResilienceServerSideStream(OrderStateStreamWrapperConfiguration.builder(executorService)
      .addOnResponseListener(newResponse -> receivedUpdates.incrementAndGet())
      .addOnConnectListener(latch::countDown)
      .build()
    );
    wrapper.subscribe(OrderStateStreamRequest.getDefaultInstance());

    latch.await();
    assertThat(receivedUpdates.get()).isEqualTo(4);
    verifyThat(OrdersStreamServiceGrpc.getOrderStateStreamMethod(), times(3));
  }


  @SneakyThrows
  @Test
  void testServerSideStreamNotReconnectByError() {
    var receivedUpdates = new AtomicInteger();
    var errorSubscriptionResponse = OrderStateStreamResponse.newBuilder()
      .setSubscription(SubscriptionResponse.newBuilder()
        .setStatus(ResultSubscriptionStatus.RESULT_SUBSCRIPTION_STATUS_ERROR)
        .build())
      .build();
    stubFor(serverStreamingMethod(OrdersStreamServiceGrpc.getOrderStateStreamMethod())
      .willReturn(
        stream(response(errorSubscriptionResponse).withFixedDelay(50))
          .and(statusException(Status.INTERNAL)) // выкидываем статус с ошибкой
      ));
    var factoryWithConfig = createStubFactory();
    var factory = factoryWithConfig._1();
    var streamFactory = StreamServiceStubFactory.create(factory);
    var executorService = Executors.newSingleThreadScheduledExecutor();
    var wrapper = streamFactory.newResilienceServerSideStream(OrderStateStreamWrapperConfiguration.builder(executorService)
      .addOnResponseListener(order -> receivedUpdates.incrementAndGet())
      .build()
    );
    wrapper.subscribe(OrderStateStreamRequest.getDefaultInstance());

    Thread.sleep(5000);
    assertThat(receivedUpdates.get()).isZero();
    verifyThat(OrdersStreamServiceGrpc.getOrderStateStreamMethod(), times(1));
  }
}
