package ru.ttech.piapi.core.impl.operations;

import ru.tinkoff.piapi.contract.v1.AccountSubscriptionStatus;
import ru.tinkoff.piapi.contract.v1.OperationsStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.PingDelaySettings;
import ru.tinkoff.piapi.contract.v1.PortfolioStreamRequest;
import ru.tinkoff.piapi.contract.v1.PortfolioStreamResponse;
import ru.tinkoff.piapi.contract.v1.PortfolioSubscriptionStatus;
import ru.ttech.piapi.core.connector.resilience.ResilienceServerSideStreamWrapper;
import ru.ttech.piapi.core.connector.streaming.ServerSideStreamConfiguration;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;

import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class PortfolioStreamWrapper extends ResilienceServerSideStreamWrapper<PortfolioStreamRequest, PortfolioStreamResponse> {

  public PortfolioStreamWrapper(
    StreamServiceStubFactory streamFactory,
    ScheduledExecutorService executorService,
    OnNextListener<PortfolioStreamResponse> onResponseListener,
    Runnable onConnectListener
  ) {
    super(streamFactory, executorService, onResponseListener, onConnectListener);
  }

  @Override
  protected ServerSideStreamConfiguration.Builder<?, PortfolioStreamRequest,
    PortfolioStreamResponse> getConfigurationBuilder(PortfolioStreamRequest request) {
    var requestWithPing = PortfolioStreamRequest.newBuilder(request)
      .setPingSettings(PingDelaySettings.newBuilder().setPingDelayMs(pingDelay).build())
      .build();
    return ServerSideStreamConfiguration.builder(
        OperationsStreamServiceGrpc::newStub,
        OperationsStreamServiceGrpc.getPortfolioStreamMethod(),
        (stub, observer) -> stub.portfolioStream(requestWithPing, observer))
      .addOnNextListener(response -> {
        if (response.hasPortfolio()) {
          onResponseListener.onNext(response);
        }
      });
  }

  @Override
  protected void processSubscriptionResult(PortfolioStreamResponse response) {
    if (response.hasSubscriptions()) {
      var successSubscriptionsList = response.getSubscriptions().getAccountsList().stream()
        .filter(subscription -> subscription.getSubscriptionStatus() == PortfolioSubscriptionStatus.PORTFOLIO_SUBSCRIPTION_STATUS_SUCCESS)
        .collect(Collectors.toList());
      if (!successSubscriptionsList.isEmpty()) {
        var filteredRequest = requestRef.get().getAccountsList().size() == successSubscriptionsList.size()
         ? requestRef.get()
           : PortfolioStreamRequest.newBuilder(requestRef.get()).clearAccounts()
         .addAllAccounts(successSubscriptionsList.stream().map(AccountSubscriptionStatus::getAccountId).collect(Collectors.toList()))
         .build();
        requestRef.set(filteredRequest);
        processSuccessSubscription();
      }
    }
  }
}
