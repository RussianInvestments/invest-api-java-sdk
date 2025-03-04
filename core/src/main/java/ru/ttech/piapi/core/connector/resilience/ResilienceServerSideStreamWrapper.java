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

  private static final Logger logger = LoggerFactory.getLogger(ResilienceServerSideStreamWrapper.class);
  protected final AtomicLong lastInteractionTime = new AtomicLong();
  protected final AtomicReference<ReqT> requestRef = new AtomicReference<>(null);
  protected final AtomicReference<ServerSideStreamWrapper<?, RespT>> streamWrapperRef = new AtomicReference<>(null);
  protected final AtomicReference<ScheduledFuture<?>> healthCheckFutureRef = new AtomicReference<>(null);
  protected final StreamServiceStubFactory streamFactory;
  protected final ScheduledExecutorService executorService;
  protected final OnNextListener<RespT> onResponseListener;
  protected final Runnable onConnectListener;
  protected final int pingDelay;
  private final int inactivityTimeout;

  public ResilienceServerSideStreamWrapper(
    StreamServiceStubFactory streamFactory,
    ScheduledExecutorService executorService,
    OnNextListener<RespT> onResponseListener,
    Runnable onConnectListener
  ) {
    this.inactivityTimeout = streamFactory.getServiceStubFactory().getConfiguration().getStreamInactivityTimeout();
    this.pingDelay = streamFactory.getServiceStubFactory().getConfiguration().getStreamPingDelay();
    this.streamFactory = streamFactory;
    this.executorService = executorService;
    this.onResponseListener = onResponseListener;
    this.onConnectListener = onConnectListener;
  }

  public final void disconnect() {
    if (healthCheckFutureRef.get() != null) {
      healthCheckFutureRef.get().cancel(true);
      healthCheckFutureRef.set(null);
    }
    Optional.ofNullable(streamWrapperRef.get())
      .ifPresent(ServerSideStreamWrapper::disconnect);
    streamWrapperRef.set(null);
    requestRef.set(null);
  }

  public void subscribe(ReqT request) {
    if (streamWrapperRef.get() != null) {
      throw new IllegalStateException("Stream was already busied");
    }
    var wrapper = streamFactory.newServerSideStream(getConfigurationBuilder(request)
      .addOnNextListener(response -> lastInteractionTime.set(System.currentTimeMillis()))
      .addOnNextListener(this::processSubscriptionResult)
      .build());
    wrapper.connect();
    requestRef.set(request);
    streamWrapperRef.set(wrapper);
    lastInteractionTime.set(System.currentTimeMillis());
  }

  protected abstract ServerSideStreamConfiguration.Builder<?, ReqT, RespT> getConfigurationBuilder(ReqT request);

  protected abstract void processSubscriptionResult(RespT response);

  protected final void processSuccessSubscription() {
    logger.info("Connected!");
    if (healthCheckFutureRef.get() == null) {
      healthCheckFutureRef.set(executorService.scheduleAtFixedRate(this::healthCheck, 0, pingDelay, TimeUnit.MILLISECONDS));
    } else if (onConnectListener != null) {
      onConnectListener.run();
    }
  }

  private void healthCheck() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastInteractionTime.get() > inactivityTimeout) {
      logger.info("Reconnecting...");
      Optional.ofNullable(streamWrapperRef.get())
        .ifPresent(ServerSideStreamWrapper::disconnect);
      streamWrapperRef.set(null);
      subscribe(requestRef.get());
    }
  }
}
