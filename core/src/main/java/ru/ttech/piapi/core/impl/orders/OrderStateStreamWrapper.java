package ru.ttech.piapi.core.impl.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamRequest;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamResponse;
import ru.tinkoff.piapi.contract.v1.OrdersStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.ResultSubscriptionStatus;
import ru.ttech.piapi.core.connector.streaming.ServerSideStreamWrapper;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
import ru.ttech.piapi.core.impl.orders.wrapper.OrderStateWrapper;

import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class OrderStateStreamWrapper {

  private static final long pingDelayMs = 20000;
  private static final Logger logger = LoggerFactory.getLogger(OrderStateStreamWrapper.class);
  protected final AtomicLong lastPingTime = new AtomicLong(0);
  protected final BlockingDeque<Boolean> connectionResult = new LinkedBlockingDeque<>(1);
  protected volatile boolean isConnected = false;
  protected final OnNextListener<OrderStateWrapper> globalOnOrderStateListener;
  protected final StreamServiceStubFactory streamFactory;
  protected final ScheduledExecutorService executorService;
  protected OrderStateStreamRequest lastSuccessRequest;
  protected ServerSideStreamWrapper<
    OrdersStreamServiceGrpc.OrdersStreamServiceStub,
    OrderStateStreamResponse> streamWrapper;

  public OrderStateStreamWrapper(
    StreamServiceStubFactory streamFactory,
    ScheduledExecutorService executorService,
    OnNextListener<OrderStateWrapper> globalOnOrderStateListener
  ) {
    this.streamFactory = streamFactory;
    this.globalOnOrderStateListener = globalOnOrderStateListener;
    this.executorService = executorService;
    this.executorService.scheduleAtFixedRate(this::healthCheck, 100, pingDelayMs, TimeUnit.MILLISECONDS);
  }

  protected void healthCheck() {
    long currentTime = System.currentTimeMillis();
    logger.debug("Health check");
    if (isConnected && currentTime - lastPingTime.get() > pingDelayMs) {
      logger.info("Reconnecting...");
      disconnect();
      subscribe(lastSuccessRequest);
    }
  }

  public void disconnect() {
    logger.info("Disconnecting...");
    Optional.ofNullable(streamWrapper)
      .ifPresent(ServerSideStreamWrapper::disconnect);
    isConnected = false;
    streamWrapper = null;
    logger.info("Disconnected!");
  }

  public synchronized void subscribe(OrderStateStreamRequest request) {
    if (isConnected) {
      throw new IllegalStateException("Stream was already connected");
    }
    logger.info("Connecting...");
    streamWrapper = streamFactory.newServerSideStream(
      OrderStateStreamConfiguration.builder(request)
        .addOrderStateListener(globalOnOrderStateListener)
        .addOnNextListener(this::waitSubscriptionResult)
        .addOnNextListener(this::pingListener)
        .build()
    );
    streamWrapper.connect();
    try {
      isConnected = connectionResult.take();
      if (isConnected) {
        lastSuccessRequest = request;
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    logger.info("Connected");
  }

  protected void waitSubscriptionResult(OrderStateStreamResponse response) {
    if (response.hasSubscription()
      && response.getSubscription().getStatus().equals(ResultSubscriptionStatus.RESULT_SUBSCRIPTION_STATUS_OK)
    ) {
      try {
        connectionResult.put(true);
      } catch (IllegalStateException ignored) {
       logger.warn("Unknown subscription got");
      } catch (InterruptedException e) {
        logger.error("Error occurred while passing subscription result to queue");
      }
    }
  }

  protected void pingListener(OrderStateStreamResponse response) {
    if (response.hasPing()) {
      logger.info("Ping response incoming");
      lastPingTime.set(System.currentTimeMillis());
    }
  }
}
