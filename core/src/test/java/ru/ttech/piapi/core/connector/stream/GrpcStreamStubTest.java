package ru.ttech.piapi.core.connector.stream;

import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import org.grpcmock.junit5.GrpcMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.contract.v1.MarketDataStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamRequest;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamResponse;
import ru.tinkoff.piapi.contract.v1.OrdersStreamServiceGrpc;
import ru.ttech.piapi.core.connector.GrpcStubBaseTest;
import ru.ttech.piapi.core.connector.streaming.BidirectionalStreamConfiguration;
import ru.ttech.piapi.core.connector.streaming.ServerSideStreamConfiguration;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;

import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import static org.grpcmock.GrpcMock.bidiStreamingMethod;
import static org.grpcmock.GrpcMock.response;
import static org.grpcmock.GrpcMock.serverStreamingMethod;
import static org.grpcmock.GrpcMock.stream;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.times;
import static org.grpcmock.GrpcMock.verifyThat;

@ExtendWith(GrpcMockExtension.class)
public class GrpcStreamStubTest extends GrpcStubBaseTest {

  private static final Logger logger = LoggerFactory.getLogger(GrpcStreamStubTest.class);

  @SneakyThrows
  @Test
  public void serverSideStream_success() {
    // setup mock stub
    var latch = new CountDownLatch(3);
    var response = OrderStateStreamResponse.getDefaultInstance();
    stubFor(serverStreamingMethod(OrdersStreamServiceGrpc.getOrderStateStreamMethod())
      .willReturn(
        stream(response(response).withFixedDelay(50))
          .and(response(response).withFixedDelay(50))
          .and(response(response).withFixedDelay(50))
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
        .addOnNextListener(markerDataResponse -> {
          logger.info("Сообщение: {}", markerDataResponse);
          latch.countDown();
        })
        .addOnErrorListener(throwable -> logger.error("Произошла ошибка: {}", throwable.getMessage()))
        .addOnCompleteListener(() -> logger.info("Стрим завершен"))
        .build()
    );
    stream.connect();

    latch.await();
    stream.disconnect();
    verifyThat(OrdersStreamServiceGrpc.getOrderStateStreamMethod(), times(1));
  }

  @SneakyThrows
  @Test
  public void bidirectionalStream_success() {
    // setup mock stub
    var latch = new CountDownLatch(1);
    stubFor(bidiStreamingMethod(MarketDataStreamServiceGrpc.getMarketDataStreamMethod())
      .withFirstRequest(req -> req.equals(MarketDataRequest.getDefaultInstance()))
      .willProxyTo(responseObserver -> new StreamObserver<>() {
        @Override
        public void onNext(MarketDataRequest marketDataRequest) {
          IntStream.range(0, 5).forEach(i -> {
            responseObserver.onNext(MarketDataResponse.getDefaultInstance());
            try {
              Thread.sleep(20);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          });
          latch.countDown();
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

    latch.await();
    stream.disconnect();
    verifyThat(MarketDataStreamServiceGrpc.getMarketDataStreamMethod(), times(1));
  }
}
