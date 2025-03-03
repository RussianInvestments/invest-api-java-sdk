package ru.ttech.piapi.core.impl.orders;

import ru.tinkoff.piapi.contract.v1.OrderStateStreamRequest;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamResponse;
import ru.tinkoff.piapi.contract.v1.OrdersStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.ResultSubscriptionStatus;
import ru.ttech.piapi.core.connector.resilience.ResilienceServerSideStreamWrapper;
import ru.ttech.piapi.core.connector.streaming.ServerSideStreamConfiguration;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;

import java.util.concurrent.ScheduledExecutorService;

public class OrderStateStreamWrapper extends ResilienceServerSideStreamWrapper<OrderStateStreamRequest, OrderStateStreamResponse> {

  public OrderStateStreamWrapper(
    StreamServiceStubFactory streamFactory,
    ScheduledExecutorService executorService,
    OnNextListener<OrderStateStreamResponse> onOrderStateListener,
    Runnable onReconnectListener
  ) {
    super(streamFactory, executorService, onOrderStateListener, onReconnectListener);
  }

  @Override
  protected ServerSideStreamConfiguration.Builder<?, OrderStateStreamRequest,
    OrderStateStreamResponse> getConfigurationBuilder(OrderStateStreamRequest request) {
      return ServerSideStreamConfiguration.builder(
          OrdersStreamServiceGrpc::newStub,
          OrdersStreamServiceGrpc.getOrderStateStreamMethod(),
          (stub, observer) -> stub.orderStateStream(request, observer))
        .addOnNextListener(response -> {
          if (response.hasOrderState()) {
            onResponseListener.onNext(response);
          }
        });
  }

  @Override
  protected void processSubscriptionResult(OrderStateStreamResponse response) {
    if (response.hasSubscription()
      && response.getSubscription().getStatus().equals(ResultSubscriptionStatus.RESULT_SUBSCRIPTION_STATUS_OK)) {
      processSuccessSubscription();
    }
  }
}
