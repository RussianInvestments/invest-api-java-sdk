package ru.tinkoff.piapi.core;

import javax.annotation.Nonnull;
import ru.tinkoff.piapi.contract.v1.GetSignalsRequest;
import ru.tinkoff.piapi.contract.v1.GetSignalsResponse;
import ru.tinkoff.piapi.contract.v1.GetStrategiesRequest;
import ru.tinkoff.piapi.contract.v1.GetStrategiesResponse;
import ru.tinkoff.piapi.contract.v1.Page;
import ru.tinkoff.piapi.contract.v1.Signal;
import ru.tinkoff.piapi.contract.v1.SignalDirection;
import ru.tinkoff.piapi.contract.v1.SignalServiceGrpc.SignalServiceBlockingStub;
import ru.tinkoff.piapi.contract.v1.SignalServiceGrpc.SignalServiceStub;
import ru.tinkoff.piapi.contract.v1.SignalState;
import ru.tinkoff.piapi.contract.v1.Strategy;
import ru.tinkoff.piapi.core.utils.Helpers;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SignalService {
  private final SignalServiceBlockingStub signalBlockingStub;
  private final SignalServiceStub signalStub;
  private final boolean sandboxMode;

  public SignalService(@Nonnull SignalServiceBlockingStub signalBlockingStub,
                       @Nonnull SignalServiceStub signalStub,
                       boolean sandboxMode) {
    this.signalBlockingStub = signalBlockingStub;
    this.signalStub = signalStub;
    this.sandboxMode = sandboxMode;
  }

  @Nonnull
  public List<Strategy> getStrategiesSync() {
    return Helpers.unaryCall(() -> signalBlockingStub.getStrategies(
      GetStrategiesRequest.newBuilder()
        .build())
      .getStrategiesList());
  }

  @Nonnull
  public List<Strategy> getStrategiesSync(@Nonnull String strategyId) {
    return Helpers.unaryCall(() -> signalBlockingStub.getStrategies(
        GetStrategiesRequest.newBuilder().setStrategyId(strategyId)
          .build())
      .getStrategiesList());
  }

  @Nonnull
  public CompletableFuture<List<Strategy>> getStrategies() {
    return Helpers.<GetStrategiesResponse>unaryAsyncCall(
        observer -> signalStub.getStrategies(
          GetStrategiesRequest.newBuilder().build(),
          observer))
      .thenApply(GetStrategiesResponse::getStrategiesList);
  }

  @Nonnull
  public CompletableFuture<List<Strategy>> getStrategies(@Nonnull String strategyId) {
    return Helpers.<GetStrategiesResponse>unaryAsyncCall(
        observer -> signalStub.getStrategies(
          GetStrategiesRequest.newBuilder().setStrategyId(strategyId).build(),
          observer))
      .thenApply(GetStrategiesResponse::getStrategiesList);
  }

  @Nonnull
  public List<Signal> getSignalsSync() {
    return Helpers.unaryCall(() -> signalBlockingStub.getSignals(
          GetSignalsRequest.newBuilder()
            .build())
      .getSignalsList());
  }

  @Nonnull
  public List<Signal> getSignalsSync(@Nonnull Page page) {
    return Helpers.unaryCall(() -> signalBlockingStub.getSignals(
          GetSignalsRequest.newBuilder().setPaging(page)
            .build()))
      .getSignalsList();
  }

  @Nonnull
  public CompletableFuture<List<Signal>> getSignals() {
    return Helpers.<GetSignalsResponse>unaryAsyncCall(
        observer -> signalStub.getSignals(
          GetSignalsRequest.newBuilder().build(),
          observer))
      .thenApply(GetSignalsResponse::getSignalsList);
  }

  @Nonnull
  public CompletableFuture<List<Signal>> getSignals(@Nonnull Page page) {
    return Helpers.<GetSignalsResponse>unaryAsyncCall(
        observer -> signalStub.getSignals(
          GetSignalsRequest.newBuilder().setPaging(page).build(),
          observer))
      .thenApply(GetSignalsResponse::getSignalsList);
  }

  @Nonnull
  public CompletableFuture<List<Signal>> getSignals(@Nonnull String strategyId) {
    return Helpers.<GetSignalsResponse>unaryAsyncCall(
        observer -> signalStub.getSignals(
          GetSignalsRequest.newBuilder().setStrategyId(strategyId).build(),
          observer))
      .thenApply(GetSignalsResponse::getSignalsList);
  }

  @Nonnull
  public CompletableFuture<List<Signal>> getSignals(@Nonnull String strategyId, @Nonnull SignalState signalState) {
    return Helpers.<GetSignalsResponse>unaryAsyncCall(
        observer -> signalStub.getSignals(
          GetSignalsRequest.newBuilder().setStrategyId(strategyId).setActive(signalState).build(),
          observer))
      .thenApply(GetSignalsResponse::getSignalsList);
  }

  @Nonnull
  public CompletableFuture<List<Signal>> getSignal(@Nonnull String signalId) {
    return Helpers.<GetSignalsResponse>unaryAsyncCall(
        observer -> signalStub.getSignals(
          GetSignalsRequest.newBuilder().setSignalId(signalId).build(),
          observer))
      .thenApply(GetSignalsResponse::getSignalsList);
  }

  @Nonnull
  public CompletableFuture<List<Signal>> getSignals(@Nonnull SignalDirection signalDirection) {
    return Helpers.<GetSignalsResponse>unaryAsyncCall(
        observer -> signalStub.getSignals(
          GetSignalsRequest.newBuilder().setDirection(signalDirection).build(),
          observer))
      .thenApply(GetSignalsResponse::getSignalsList);
  }
}
