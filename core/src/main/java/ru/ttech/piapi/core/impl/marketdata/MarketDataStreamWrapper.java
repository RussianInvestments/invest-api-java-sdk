package ru.ttech.piapi.core.impl.marketdata;

import io.vavr.Lazy;
import io.vavr.collection.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.contract.v1.MarketDataStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.PingDelaySettings;
import ru.ttech.piapi.core.connector.streaming.BidirectionalStreamWrapper;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;
import ru.ttech.piapi.core.impl.marketdata.subscription.MarketDataSubscriptionResult;
import ru.ttech.piapi.core.impl.marketdata.subscription.RequestAction;
import ru.ttech.piapi.core.impl.marketdata.util.MarketDataRequestUtil;
import ru.ttech.piapi.core.impl.marketdata.util.MarketDataResponseUtil;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Resilience-обёртка над {@link BidirectionalStreamWrapper}.
 * Переподключает стрим при разрыве соединения (превышении inactivity-timeout) и повторно подписывается на обновления
 */
public class MarketDataStreamWrapper {

  protected static final Logger logger = LoggerFactory.getLogger(MarketDataStreamWrapper.class);

  private final UUID uuid = UUID.randomUUID();
  protected final AtomicLong lastInteractionTime = new AtomicLong();
  protected final LinkedBlockingQueue<MarketDataSubscriptionResult> subscriptionResultsQueue = new LinkedBlockingQueue<>();
  protected final AtomicInteger subscriptionsCount = new AtomicInteger(0);
  protected final Map<MarketDataResponseType, Set<Instrument>> subscriptionsMap = Map.of(
    MarketDataResponseType.CANDLE, ConcurrentHashMap.newKeySet(),
    MarketDataResponseType.LAST_PRICE, ConcurrentHashMap.newKeySet(),
    MarketDataResponseType.TRADE, ConcurrentHashMap.newKeySet(),
    MarketDataResponseType.ORDER_BOOK, ConcurrentHashMap.newKeySet(),
    MarketDataResponseType.TRADING_STATUS, ConcurrentHashMap.newKeySet()
  );
  protected final AtomicReference<List<MarketDataRequest>> requestsRef = new AtomicReference<>(List.empty());
  protected final AtomicReference<ScheduledFuture<?>> healthCheckFutureRef = new AtomicReference<>(null);
  protected final AtomicReference<CompletableFuture<MarketDataSubscriptionResult>> lastTask = new AtomicReference<>();
  protected final AtomicBoolean isResubscribing = new AtomicBoolean(false);
  protected final int pingDelay;
  protected final int inactivityTimeout;
  protected final BidirectionalStreamWrapper<
    MarketDataStreamServiceGrpc.MarketDataStreamServiceStub,
    MarketDataRequest,
    MarketDataResponse> streamWrapper;
  protected final ScheduledExecutorService executorService;
  protected final List<Runnable> onConnectListeners;

  public MarketDataStreamWrapper(
    StreamServiceStubFactory streamFactory,
    MarketDataStreamWrapperConfiguration configuration
  ) {
    this.streamWrapper = streamFactory.newBidirectionalStream(configuration.getStreamWrapperConfigBuilder()
      .addOnNextListener(this::processResponse)
      .build());
    this.onConnectListeners = List.ofAll(configuration.getOnConnectListeners());
    this.executorService = configuration.getExecutorService();
    this.inactivityTimeout = streamFactory.getServiceStubFactory().getConfiguration().getStreamInactivityTimeout();
    this.pingDelay = streamFactory.getServiceStubFactory().getConfiguration().getStreamPingDelay();
    this.lastTask.set(CompletableFuture.completedFuture(null));
  }

  /**
   * Останавливает healthcheck, очищает очередь запросов и отключается от стрима
   */
  public void disconnect() {
    if (healthCheckFutureRef.get() != null) {
      healthCheckFutureRef.get().cancel(true);
      healthCheckFutureRef.set(null);
    }
    requestsRef.updateAndGet(requests -> List.empty());
    subscriptionsMap.values().forEach(Set::clear);
    updateSubscriptionsCount();
    disconnectWrapper();
  }

  protected void disconnectWrapper() {
    streamWrapper.disconnect();
  }

  protected void sendPingSettings() {
    var pingRequest = MarketDataRequest.newBuilder()
      .setPingSettings(PingDelaySettings.newBuilder()
        .setPingDelayMs(pingDelay)
        .build())
      .build();
    streamWrapper.newCall(pingRequest);
  }

  /**
   * Метод для отправки запроса в стрим. Можно подписаться или отписаться от обновлений
   *
   * @param request запрос на подписку или отписку
   * @return результат выполнения запроса
   */
  public CompletableFuture<MarketDataSubscriptionResult> newCall(MarketDataRequest request) {
    var supplier = Lazy.of(() -> CompletableFuture.supplyAsync(() -> {
      var requestType = MarketDataRequestUtil.determineRequestType(request);
      subscriptionResultsQueue.clear();
      if (!streamWrapper.isConnected()) {
        streamWrapper.connect();
      }
      streamWrapper.newCall(request);
      lastInteractionTime.set(System.currentTimeMillis());
      if (!isResubscribing.get()) {
        requestsRef.updateAndGet(requests -> requests.append(request));
      }
      return waitSubscriptionResult(requestType, MarketDataRequestUtil.extractInstruments(request));
    }));
    return lastTask.updateAndGet(previousTask ->
      previousTask
        .thenCompose(previousResult -> supplier.get())
        .exceptionally(ex -> null)
    );
  }

  protected MarketDataSubscriptionResult waitSubscriptionResult(
    MarketDataResponseType responseType,
    Collection<Instrument> instruments
  ) {
    try {
      var subscriptionResult = subscriptionResultsQueue.poll(inactivityTimeout, TimeUnit.MILLISECONDS);
      if (subscriptionResult == null || (subscriptionResult.getResponseType() != responseType
        || !subscriptionResult.getSubscriptionStatusMap().keySet().containsAll(instruments))
      ) {
        throw new IllegalStateException("Wrong subscription result!");
      }
      return subscriptionResult;
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while waiting for subscription response", e);
    }
  }

  protected void processResponse(MarketDataResponse response) {
    lastInteractionTime.set(System.currentTimeMillis());
    MarketDataResponseUtil.getSubscriptionResultFromResponse(response).ifPresent(this::processSubscriptionResult);
  }

  protected void processSubscriptionResult(MarketDataSubscriptionResult subscriptionResult) {
    try {
      updateSubscriptionsMap(subscriptionResult);
      if (subscriptionsCount.get() > 0) {
        sendPingSettings();
        if (healthCheckFutureRef.get() == null) {
          logger.debug("Healthcheck enabled in wrapper {}", uuid);
          onConnectListeners.forEach(Runnable::run);
          healthCheckFutureRef.set(executorService.scheduleAtFixedRate(this::healthCheck, 0, pingDelay, TimeUnit.MILLISECONDS));
        }
      } else if (!isResubscribing.get() && healthCheckFutureRef.get() != null) {
        logger.info("Healthcheck disabled in wrapper {}", uuid);
        healthCheckFutureRef.get().cancel(true);
        healthCheckFutureRef.set(null);
      }
      subscriptionResultsQueue.put(subscriptionResult);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Метод для проверки статуса подписки на определённый тип обновлений по инструменту
   *
   * @param responseType Тип обновлений
   * @param instrument   Инструмент
   * @return true, если подписка есть, false - если нет
   */
  public boolean isSubscribed(MarketDataResponseType responseType, Instrument instrument) {
    return getSubscribedInstruments(responseType).contains(instrument);
  }

  protected Set<Instrument> getSubscribedInstruments(MarketDataResponseType responseType) {
    if (!subscriptionsMap.containsKey(responseType)) {
      throw new IllegalArgumentException("Unsupported response type: " + responseType);
    }
    return subscriptionsMap.get(responseType);
  }

  /**
   * Метод актуализации подписок враппера
   *
   * @param subscriptionResult - результат подписки, полученный от сервиса
   */
  protected void updateSubscriptionsMap(MarketDataSubscriptionResult subscriptionResult) {
    var currentAction = subscriptionResult.getSubscriptionAction();
    var subscribedInstruments = getSubscribedInstruments(subscriptionResult.getResponseType());
    var errorResults = subscriptionResult.getSubscriptionStatusMap().entrySet().stream()
      .filter(entry ->  entry.getValue().isError())
      .map(Map.Entry::getKey)
      .collect(Collectors.toSet());
    var successResults = subscriptionResult.getSubscriptionStatusMap().entrySet().stream()
      .filter(entry -> entry.getValue().isOk())
      .peek(entry -> {
        if (currentAction == RequestAction.SUBSCRIBE) {
          logger.debug("Success subscribed in wrapper {} for instrument: {}", uuid, entry.getKey().getInstrumentUid());
        } else if (currentAction == RequestAction.UNSUBSCRIBE) {
          logger.debug("Success unsubscribed in wrapper {} for instrument: {}", uuid, entry.getKey().getInstrumentUid());
        }
      })
      .map(Map.Entry::getKey)
      .collect(Collectors.toSet());
    if (currentAction == RequestAction.SUBSCRIBE) {
      subscribedInstruments.addAll(successResults);
    } else if (currentAction == RequestAction.UNSUBSCRIBE) {
      successResults.forEach(subscribedInstruments::remove);
    }
    errorResults.forEach(subscribedInstruments::remove);
    updateSubscriptionsCount();
  }

  /**
   * Метод актуализации количества подписок.
   * Обновляет количество подписок враппера исходя из текущего состояния subscriptionsMap
   */
  protected void updateSubscriptionsCount() {
    int currentSize = subscriptionsMap.values().stream().mapToInt(Set::size).sum();
    subscriptionsCount.set(currentSize);
  }

  protected void healthCheck() {
    long currentTime = System.currentTimeMillis();
    boolean timeoutExceeded = currentTime - lastInteractionTime.get() > inactivityTimeout;
    if (timeoutExceeded && subscriptionsCount.get() > 0) {
      logger.info("Wrapper {} reconnecting...", uuid);
      disconnectWrapper();
      isResubscribing.set(true);
      requestsRef.get().forEach(request -> newCall(request).join());
      isResubscribing.set(false);
      lastInteractionTime.set(System.currentTimeMillis());
      if (subscriptionsCount.get() > 0) {
        onConnectListeners.forEach(Runnable::run);
      }
    }
  }

  protected int getSubscriptionsCount() {
    return subscriptionsCount.get();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MarketDataStreamWrapper that = (MarketDataStreamWrapper) o;
    return uuid.equals(that.uuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid);
  }
}
