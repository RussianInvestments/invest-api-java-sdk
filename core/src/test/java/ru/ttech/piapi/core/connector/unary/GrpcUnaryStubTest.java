package ru.ttech.piapi.core.connector.unary;

import io.grpc.Status;
import org.grpcmock.GrpcMock;
import org.grpcmock.junit5.GrpcMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.tinkoff.piapi.contract.v1.GetLastPricesRequest;
import ru.tinkoff.piapi.contract.v1.GetLastPricesResponse;
import ru.tinkoff.piapi.contract.v1.MarketDataServiceGrpc;
import ru.ttech.piapi.core.connector.GrpcStubBaseTest;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.grpcmock.GrpcMock.calledMethod;
import static org.grpcmock.GrpcMock.statusException;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.times;
import static org.grpcmock.GrpcMock.unaryMethod;
import static org.grpcmock.GrpcMock.verifyThat;

@ExtendWith(GrpcMockExtension.class)
public class GrpcUnaryStubTest extends GrpcStubBaseTest {

  @Test
  public void contextFork_success() {
    // arrange stub and data
    var request = GetLastPricesRequest.getDefaultInstance();
    var priceResponse = GetLastPricesResponse.getDefaultInstance();
    stubFor(unaryMethod(MarketDataServiceGrpc.getGetLastPricesMethod())
      .withRequest(request)
      .willReturn(statusException(Status.CANCELLED).withFixedDelay(100))
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
}
