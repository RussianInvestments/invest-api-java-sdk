package ru.ttech.piapi.core.impl.orders;

import ru.tinkoff.piapi.contract.v1.OrdersStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.ResultSubscriptionStatus;
import ru.tinkoff.piapi.contract.v1.TradesStreamRequest;
import ru.tinkoff.piapi.contract.v1.TradesStreamResponse;
import ru.ttech.piapi.core.connector.resilience.ResilienceServerSideStreamWrapper;
import ru.ttech.piapi.core.connector.streaming.ServerSideStreamConfiguration;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;

import java.util.concurrent.ScheduledExecutorService;

public class TradesStreamWrapper extends ResilienceServerSideStreamWrapper<TradesStreamRequest, TradesStreamResponse> {

  public TradesStreamWrapper(
    StreamServiceStubFactory streamFactory,
    ScheduledExecutorService executorService,
    OnNextListener<TradesStreamResponse> onTradeListener,
    Runnable onReconnectListener
  ) {
    super(streamFactory, executorService, onTradeListener, onReconnectListener);
  }

  @Override
  protected ServerSideStreamConfiguration.Builder<?, TradesStreamRequest,
    TradesStreamResponse> getConfigurationBuilder(TradesStreamRequest request) {
    var requestWithPing = TradesStreamRequest.newBuilder(request).setPingDelayMs(pingDelay).build();
    return ServerSideStreamConfiguration.builder(
        OrdersStreamServiceGrpc::newStub,
        OrdersStreamServiceGrpc.getTradesStreamMethod(),
        (stub, observer) -> stub.tradesStream(requestWithPing, observer))
      .addOnNextListener(response -> {
        if (response.hasOrderTrades()) {
          onResponseListener.onNext(response);
        }
      });
  }

  @Override
  protected void processSubscriptionResult(TradesStreamResponse response) {
    if (response.hasSubscription()
      && response.getSubscription().getStatus() == ResultSubscriptionStatus.RESULT_SUBSCRIPTION_STATUS_OK) {
      processSuccessSubscription();
    }
  }
}
