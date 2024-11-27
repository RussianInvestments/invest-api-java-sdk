package ru.tinkoff.piapi.core;

import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import ru.tinkoff.piapi.contract.v1.GetSignalsRequest;
import ru.tinkoff.piapi.contract.v1.GetSignalsResponse;
import ru.tinkoff.piapi.contract.v1.GetStrategiesRequest;
import ru.tinkoff.piapi.contract.v1.GetStrategiesResponse;
import ru.tinkoff.piapi.contract.v1.Signal;
import ru.tinkoff.piapi.contract.v1.SignalServiceGrpc;
import ru.tinkoff.piapi.contract.v1.Strategy;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SignalServiceTest extends GrpcClientTester<SignalService> {
  @Override
  protected SignalService createClient(Channel channel) {
    return new SignalService(SignalServiceGrpc.newBlockingStub(channel), SignalServiceGrpc.newStub(channel), false);
  }

  @Test
  void getStrategies_Test() {
    var expected = List.of(
      Strategy.newBuilder().setStrategyId("st1").setStrategyName("Stochastic RSI").build(),
      Strategy.newBuilder().setStrategyId("st2").setStrategyName("Aroon").build()
    );

    var grpcService = mock(SignalServiceGrpc.SignalServiceImplBase.class, delegatesTo(
      new SignalServiceGrpc.SignalServiceImplBase() {
        @Override
        public void getStrategies(GetStrategiesRequest request, StreamObserver<GetStrategiesResponse> responseObserver) {
          responseObserver.onNext(GetStrategiesResponse.newBuilder().addAllStrategies(expected).build());
          responseObserver.onCompleted();
        }
      }
    ));

    var service = mkClientBasedOnServer(grpcService);

    var actualSync = service.getStrategiesSync();
    var actualAsync = service.getStrategies().join();

    assertIterableEquals(expected, actualSync);
    assertIterableEquals(expected, actualAsync);

    var inArg = GetStrategiesRequest.newBuilder().build();
    verify(grpcService, times(2)).getStrategies(eq(inArg), any());
  }

  @Test
  void getSignals_Test() {
    var expected = List.of(
      Signal.newBuilder().setStrategyId("st1").setStrategyName("Stochastic RSI").build(),
      Signal.newBuilder().setStrategyId("st2").setStrategyName("Aroon").build()
    );

    var grpcService = mock(SignalServiceGrpc.SignalServiceImplBase.class, delegatesTo(
      new SignalServiceGrpc.SignalServiceImplBase() {
        @Override
        public void getSignals(GetSignalsRequest request, StreamObserver<GetSignalsResponse> responseObserver) {
          responseObserver.onNext(GetSignalsResponse.newBuilder().addAllSignals(expected).build());
          responseObserver.onCompleted();
        }
      }
    ));

    var service = mkClientBasedOnServer(grpcService);

    var actualSync = service.getSignalsSync();
    var actualAsync = service.getSignals().join();

    assertIterableEquals(expected, actualSync);
    assertIterableEquals(expected, actualAsync);

    var inArg = GetSignalsRequest.newBuilder().build();
    verify(grpcService, times(2)).getSignals(eq(inArg), any());
  }
}
