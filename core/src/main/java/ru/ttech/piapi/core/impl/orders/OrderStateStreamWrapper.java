package ru.ttech.piapi.core.impl.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamRequest;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamResponse;
import ru.tinkoff.piapi.contract.v1.OrdersStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.ResultSubscriptionStatus;
import ru.ttech.piapi.core.connector.streaming.ServerSideStreamConfiguration;
import ru.ttech.piapi.core.connector.streaming.ServerSideStreamWrapper;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class OrderStateStreamWrapper {

  private static final long timeout = 15000;
  private static final Logger logger = LoggerFactory.getLogger(OrderStateStreamWrapper.class);
  protected final AtomicLong lastInteractionTime = new AtomicLong(0);
  protected final AtomicReference<OrderStateStreamRequest> lastSuccessRequestRef = new AtomicReference<>(null);
  protected final AtomicReference<OrderStateStreamRequest> lastRequestRef = new AtomicReference<>(null);
  protected final AtomicReference<ServerSideStreamWrapper<
    OrdersStreamServiceGrpc.OrdersStreamServiceStub,
    OrderStateStreamResponse>> streamWrapperRef = new AtomicReference<>(null);
  protected final OnNextListener<OrderStateStreamResponse> onOrderStateListener;
  protected final StreamServiceStubFactory streamFactory;
  protected final ScheduledExecutorService executorService;
  protected ScheduledFuture<?> healthCheckFuture;
  protected Runnable onReconnectAction;

  public OrderStateStreamWrapper(
    StreamServiceStubFactory streamFactory,
    ScheduledExecutorService executorService,
    OnNextListener<OrderStateStreamResponse> onOrderStateListener
  ) {
    this.streamFactory = streamFactory;
    this.onOrderStateListener = onOrderStateListener;
    this.executorService = executorService;
  }

  public void setOnReconnectAction(Runnable runnable) {
    this.onReconnectAction = runnable;
  }

  protected void healthCheck() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastInteractionTime.get() > timeout) {
      logger.info("Reconnecting...");
      var request = lastSuccessRequestRef.get() != null
        ? lastSuccessRequestRef.get()
        : lastRequestRef.get();
      disconnect();
      subscribe(request);
    }
  }

  public void disconnect() {
    Optional.ofNullable(streamWrapperRef.get())
      .ifPresent(ServerSideStreamWrapper::disconnect);
    streamWrapperRef.set(null);
  }

  public void subscribe(OrderStateStreamRequest request) {
    if (streamWrapperRef.get() != null) {
      logger.warn("Stream was already busied");
      return;
    }
    var streamWrapper = streamFactory.newServerSideStream(
      ServerSideStreamConfiguration.builder(
          OrdersStreamServiceGrpc::newStub,
          OrdersStreamServiceGrpc.getOrderStateStreamMethod(),
          (stub, observer) -> stub.orderStateStream(request, observer))
        .addOnNextListener(response -> {
          if (response.hasOrderState()) {
            onOrderStateListener.onNext(response);
          }
        })
        .addOnNextListener(this::waitSubscriptionResult)
        .addOnNextListener(this::pingListener)
        .addOnCompleteListener(() -> logger.info("Stream complete"))
        .build());
    streamWrapper.connect();
    lastRequestRef.set(request);
    streamWrapperRef.set(streamWrapper);
    lastInteractionTime.set(System.currentTimeMillis());
  }

  protected void waitSubscriptionResult(OrderStateStreamResponse response) {
    if (response.hasSubscription()
      && response.getSubscription().getStatus().equals(ResultSubscriptionStatus.RESULT_SUBSCRIPTION_STATUS_OK)
    ) {
      lastSuccessRequestRef.set(lastRequestRef.get());
      lastInteractionTime.set(System.currentTimeMillis());
      if (healthCheckFuture == null) {
        healthCheckFuture = executorService.scheduleAtFixedRate(this::healthCheck, 100, 1000, TimeUnit.MILLISECONDS);
      } else if (onReconnectAction != null) {
        onReconnectAction.run();
      }
    }
  }

  protected void pingListener(OrderStateStreamResponse response) {
    if (response.hasPing()) {
      lastInteractionTime.set(System.currentTimeMillis());
    }
  }
}
