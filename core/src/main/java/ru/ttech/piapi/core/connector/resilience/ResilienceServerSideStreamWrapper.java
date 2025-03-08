package ru.ttech.piapi.core.connector.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ttech.piapi.core.connector.streaming.ServerSideStreamWrapper;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Resilience-обёртка над {@link ServerSideStreamWrapper}.
 * Переподключает стрим при разрыве соединения (превышении inactivity-timeout) и повторно подписывается на обновления
 *
 * @param <ReqT>
 * @param <RespT>
 */
public class ResilienceServerSideStreamWrapper<ReqT, RespT> {

  private static final Logger logger = LoggerFactory.getLogger(ResilienceServerSideStreamWrapper.class);
  private final AtomicLong lastInteractionTime = new AtomicLong();
  private final AtomicReference<ReqT> requestRef = new AtomicReference<>(null);
  private final AtomicReference<ServerSideStreamWrapper<?, RespT>> streamWrapperRef = new AtomicReference<>(null);
  private final AtomicReference<ScheduledFuture<?>> healthCheckFutureRef = new AtomicReference<>(null);
  private final StreamServiceStubFactory streamFactory;
  private final ResilienceServerSideStreamWrapperConfiguration<ReqT, RespT> configuration;
  private final int pingDelay;
  private final int inactivityTimeout;

  public ResilienceServerSideStreamWrapper(
    StreamServiceStubFactory streamFactory,
    ResilienceServerSideStreamWrapperConfiguration<ReqT, RespT> configuration
  ) {
    this.streamFactory = streamFactory;
    this.configuration = configuration;
    this.inactivityTimeout = streamFactory.getServiceStubFactory().getConfiguration().getStreamInactivityTimeout();
    this.pingDelay = streamFactory.getServiceStubFactory().getConfiguration().getStreamPingDelay();
  }

  /**
   * Метод для завершения стрима. Останавливает автоматическую проверку статуса соединения и закрывает стрим
   */
  public void disconnect() {
    if (healthCheckFutureRef.get() != null) {
      healthCheckFutureRef.get().cancel(true);
      healthCheckFutureRef.set(null);
    }
    Optional.ofNullable(streamWrapperRef.get())
      .ifPresent(ServerSideStreamWrapper::disconnect);
    streamWrapperRef.set(null);
    requestRef.set(null);
  }

  /**
   * Метод для подписки в стриме. Вызывается только во враппере, в котором ещё не была вызвана подписка
   *
   * @param request Запрос на подписку
   */
  public void subscribe(ReqT request) {
    if (request == null) {
      throw new IllegalStateException("Subscription request should not be null");
    }
    if (streamWrapperRef.get() != null) {
      throw new IllegalStateException("Stream was already busied!");
    }
    var configurationBuilder = configuration.getConfigurationBuilder(pingDelay).apply(request);
    var wrapper = streamFactory.newServerSideStream(configurationBuilder
      .addOnNextListener(response -> lastInteractionTime.set(System.currentTimeMillis()))
      .addOnNextListener(this::processSubscriptionResult)
      .build());
    wrapper.connect();
    requestRef.set(request);
    streamWrapperRef.set(wrapper);
    lastInteractionTime.set(System.currentTimeMillis());
  }

  protected void processSubscriptionResult(RespT response) {
    configuration.getSubscriptionResultProcessor().apply(requestRef.get(), response).ifPresent(successRequest -> {
      requestRef.set(successRequest);
      processSuccessSubscription();
    });
  }

  protected void processSuccessSubscription() {
    logger.info("Connected!");
    if (healthCheckFutureRef.get() == null) {
      var executorService = configuration.getExecutorService();
      healthCheckFutureRef.set(executorService.scheduleAtFixedRate(this::healthCheck, 0, pingDelay, TimeUnit.MILLISECONDS));
    }
    configuration.getOnConnectListeners().forEach(Runnable::run);
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
