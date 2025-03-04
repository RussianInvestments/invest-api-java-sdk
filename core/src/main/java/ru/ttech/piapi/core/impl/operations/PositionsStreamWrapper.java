package ru.ttech.piapi.core.impl.operations;

import ru.tinkoff.piapi.contract.v1.OperationsStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.PingDelaySettings;
import ru.tinkoff.piapi.contract.v1.PositionsAccountSubscriptionStatus;
import ru.tinkoff.piapi.contract.v1.PositionsStreamRequest;
import ru.tinkoff.piapi.contract.v1.PositionsStreamResponse;
import ru.tinkoff.piapi.contract.v1.PositionsSubscriptionStatus;
import ru.ttech.piapi.core.connector.resilience.ResilienceServerSideStreamWrapper;
import ru.ttech.piapi.core.connector.streaming.ServerSideStreamConfiguration;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;

import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class PositionsStreamWrapper extends ResilienceServerSideStreamWrapper<PositionsStreamRequest, PositionsStreamResponse> {

  public PositionsStreamWrapper(
    StreamServiceStubFactory streamFactory,
    ScheduledExecutorService executorService,
    OnNextListener<PositionsStreamResponse> onResponseListener,
    Runnable onConnectListener
  ) {
    super(streamFactory, executorService, onResponseListener, onConnectListener);
  }

  @Override
  protected ServerSideStreamConfiguration.Builder<?, PositionsStreamRequest,
    PositionsStreamResponse> getConfigurationBuilder(PositionsStreamRequest request) {
    var requestWithPing = PositionsStreamRequest.newBuilder(request)
      .setPingSettings(PingDelaySettings.newBuilder().setPingDelayMs(pingDelay).build())
      .build();
    return ServerSideStreamConfiguration.builder(
        OperationsStreamServiceGrpc::newStub,
        OperationsStreamServiceGrpc.getPositionsStreamMethod(),
        (stub, observer) -> stub.positionsStream(requestWithPing, observer))
      .addOnNextListener(response -> {
        if (response.hasPosition() || response.hasInitialPositions()) {
          onResponseListener.onNext(response);
        }
      });
  }

  @Override
  protected void processSubscriptionResult(PositionsStreamResponse response) {
    if (response.hasSubscriptions()) {
      var successSubscriptionsList = response.getSubscriptions().getAccountsList().stream()
        .filter(subscription -> subscription.getSubscriptionStatus() == PositionsAccountSubscriptionStatus.POSITIONS_SUBSCRIPTION_STATUS_SUCCESS)
        .collect(Collectors.toList());
      if (!successSubscriptionsList.isEmpty()) {
        var filteredRequest = requestRef.get().getAccountsList().size() == successSubscriptionsList.size()
          ? requestRef.get()
          : PositionsStreamRequest.newBuilder(requestRef.get()).clearAccounts()
          .addAllAccounts(successSubscriptionsList.stream().map(PositionsSubscriptionStatus::getAccountId).collect(Collectors.toList()))
          .build();
        requestRef.set(filteredRequest);
        processSuccessSubscription();
      }
    }
  }
}
