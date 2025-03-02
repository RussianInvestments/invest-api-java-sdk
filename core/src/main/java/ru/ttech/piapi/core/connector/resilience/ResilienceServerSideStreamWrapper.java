package ru.ttech.piapi.core.connector.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public abstract class ResilienceServerSideStreamWrapper<ReqT, RespT> {

  private static final long timeout = 15000;
  private static final Logger logger = LoggerFactory.getLogger(ResilienceServerSideStreamWrapper.class);
  protected final AtomicLong lastInteractionTime = new AtomicLong(0);
  protected final AtomicReference<ReqT> lastSuccessRequestRef = new AtomicReference<>(null);
  protected final AtomicReference<ReqT> lastRequestRef = new AtomicReference<>(null);
  protected final AtomicReference<ServerSideStreamWrapper<?, RespT>> streamWrapperRef = new AtomicReference<>(null);
  protected final StreamServiceStubFactory streamFactory;
  protected final ScheduledExecutorService executorService;
  protected final OnNextListener<RespT> onResponseListener;
  protected final Runnable onReconnectListener;
  protected ScheduledFuture<?> healthCheckFuture;

  public ResilienceServerSideStreamWrapper(
    StreamServiceStubFactory streamFactory,
    ScheduledExecutorService executorService,
    OnNextListener<RespT> onResponseListener,
    Runnable onReconnectListener
  ) {
    this.streamFactory = streamFactory;
    this.executorService = executorService;
    this.onResponseListener = onResponseListener;
    this.onReconnectListener = onReconnectListener;
  }

  public final void disconnect() {
    Optional.ofNullable(streamWrapperRef.get())
      .ifPresent(ServerSideStreamWrapper::disconnect);
    streamWrapperRef.set(null);
  }

  public void subscribe(ReqT request) {
    if (streamWrapperRef.get() != null) {
      logger.warn("Stream was already busied");
      return;
    }
    var wrapper = streamFactory.newServerSideStream(getConfigurationBuilder(request)
      .addOnNextListener(this::processPingResponse)
      .addOnNextListener(this::processSubscriptionResult)
      .build());
    wrapper.connect();
    lastRequestRef.set(request);
    streamWrapperRef.set(wrapper);
    lastInteractionTime.set(System.currentTimeMillis());
  }

  protected abstract ServerSideStreamConfiguration.Builder<?, ReqT, RespT> getConfigurationBuilder(ReqT request);

  protected abstract void processSubscriptionResult(RespT response);

  protected abstract void processPingResponse(RespT response);

  protected final void processSuccessSubscription() {
    logger.info("Connected!");
    lastSuccessRequestRef.set(lastRequestRef.get());
    lastInteractionTime.set(System.currentTimeMillis());
    if (healthCheckFuture == null) {
      healthCheckFuture = executorService.scheduleAtFixedRate(this::healthCheck, 0, 1000, TimeUnit.MILLISECONDS);
    } else if (onReconnectListener != null) {
      onReconnectListener.run();
    }
  }

  private void healthCheck() {
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
}
