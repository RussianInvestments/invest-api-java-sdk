package ru.ttech.piapi.core.impl.marketdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.contract.v1.MarketDataStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.PingDelaySettings;
import ru.ttech.piapi.core.connector.streaming.BidirectionalStreamWrapper;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;
import ru.ttech.piapi.core.impl.marketdata.subscription.MarketDataSubscriptionResult;
import ru.ttech.piapi.core.impl.marketdata.subscription.SubscriptionResultMapper;
import ru.ttech.piapi.core.impl.marketdata.subscription.SubscriptionStatus;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.LastPriceWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.OrderBookWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.TradeWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.TradingStatusWrapper;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

class MarketDataStreamWrapper {

  private static final Logger logger = LoggerFactory.getLogger(MarketDataStreamWrapper.class);
  protected final AtomicLong lastInteractionTime = new AtomicLong();
  protected final LinkedBlockingQueue<MarketDataSubscriptionResult> subscriptionResultsQueue = new LinkedBlockingQueue<>();
  protected final AtomicInteger subscriptionsCount = new AtomicInteger(0);
  protected final Map<MarketDataResponseType, Map<Instrument, SubscriptionStatus>> subscriptionsMap = new ConcurrentHashMap<>(Map.of(
    MarketDataResponseType.CANDLE, new ConcurrentHashMap<>(),
    MarketDataResponseType.LAST_PRICE, new ConcurrentHashMap<>(),
    MarketDataResponseType.TRADE, new ConcurrentHashMap<>(),
    MarketDataResponseType.ORDER_BOOK, new ConcurrentHashMap<>(),
    MarketDataResponseType.TRADING_STATUS, new ConcurrentHashMap<>()
  ));
  private final List<MarketDataRequest> requests = Collections.synchronizedList(new ArrayList<>());
  protected final AtomicReference<ScheduledFuture<?>> healthCheckFutureRef = new AtomicReference<>(null);
  protected final int pingDelay;
  protected final int inactivityTimeout;
  protected final BidirectionalStreamWrapper<
    MarketDataStreamServiceGrpc.MarketDataStreamServiceStub,
    MarketDataRequest,
    MarketDataResponse> streamWrapper;
  protected final ScheduledExecutorService executorService;

  protected MarketDataStreamWrapper(
    ScheduledExecutorService executorService,
    StreamServiceStubFactory streamFactory,
    OnNextListener<CandleWrapper> globalOnCandleListener,
    OnNextListener<LastPriceWrapper> globalOnLastPriceListener,
    OnNextListener<OrderBookWrapper> globalOnOrderBookListener,
    OnNextListener<TradeWrapper> globalOnTradeListener,
    OnNextListener<TradingStatusWrapper> globalOnTradingStatusListener
  ) {
    this.streamWrapper = streamFactory.newBidirectionalStream(
      MarketDataStreamConfiguration.builder()
        .addOnCandleListener(globalOnCandleListener)
        .addOnLastPriceListener(globalOnLastPriceListener)
        .addOnOrderBookListener(globalOnOrderBookListener)
        .addOnTradeListener(globalOnTradeListener)
        .addOnTradingStatusListener(globalOnTradingStatusListener)
        .addOnNextListener(this::processResponse)
        .build());
    this.streamWrapper.connect();
    this.executorService = executorService;
    this.inactivityTimeout = streamFactory.getServiceStubFactory().getConfiguration().getStreamInactivityTimeout();
    this.pingDelay = streamFactory.getServiceStubFactory().getConfiguration().getStreamPingDelay();
  }

  protected int getSubscriptionsCount() {
    return subscriptionsCount.get();
  }

  protected void disconnect() {
    if (healthCheckFutureRef.get() != null) {
      healthCheckFutureRef.get().cancel(true);
      healthCheckFutureRef.set(null);
    }
    disconnectWrapper();
  }

  protected MarketDataSubscriptionResult subscribe(MarketDataRequest request) {
    var requestType = determineRequestType(request);
    subscriptionResultsQueue.clear();
    streamWrapper.newCall(request);
    if (!requests.contains(request)) {
      requests.add(request);
    }
    return waitSubscriptionResult(requestType, extractInstruments(request));
  }

  protected MarketDataResponseType determineRequestType(MarketDataRequest request) {
    if (request.hasSubscribeCandlesRequest()) {
      return MarketDataResponseType.CANDLE;
    } else if (request.hasSubscribeLastPriceRequest()) {
      return MarketDataResponseType.LAST_PRICE;
    } else if (request.hasSubscribeOrderBookRequest()) {
      return MarketDataResponseType.ORDER_BOOK;
    } else if (request.hasSubscribeTradesRequest()) {
      return MarketDataResponseType.TRADE;
    } else if (request.hasSubscribeInfoRequest()) {
      return MarketDataResponseType.TRADING_STATUS;
    }
    return MarketDataResponseType.OTHER;
  }

  protected List<Instrument> extractInstruments(MarketDataRequest request) {
    if (request.hasSubscribeCandlesRequest()) {
      return request.getSubscribeCandlesRequest().getInstrumentsList().stream()
        .map(instrument -> new Instrument(instrument.getInstrumentId(), instrument.getInterval()))
        .collect(Collectors.toList());
    } else if (request.hasSubscribeLastPriceRequest()) {
      return request.getSubscribeLastPriceRequest().getInstrumentsList().stream()
        .map(instrument -> new Instrument(instrument.getInstrumentId()))
        .collect(Collectors.toList());
    } else if (request.hasSubscribeOrderBookRequest()) {
      return request.getSubscribeOrderBookRequest().getInstrumentsList().stream()
        .map(instrument -> new Instrument(instrument.getInstrumentId(), instrument.getDepth(), instrument.getOrderBookType()))
        .collect(Collectors.toList());
    } else if (request.hasSubscribeTradesRequest()) {
      return request.getSubscribeTradesRequest().getInstrumentsList().stream()
        .map(instrument -> new Instrument(instrument.getInstrumentId()))
        .collect(Collectors.toList());
    } else if (request.hasSubscribeInfoRequest()) {
      return request.getSubscribeInfoRequest().getInstrumentsList().stream()
        .map(instrument -> new Instrument(instrument.getInstrumentId()))
        .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  public Map<Instrument, SubscriptionStatus> getSubscriptionsMap(MarketDataResponseType responseType) {
    if (!subscriptionsMap.containsKey(responseType)) {
      throw new IllegalArgumentException("Unsupported response type: " + responseType);
    }
    return subscriptionsMap.get(responseType);
  }

  protected MarketDataSubscriptionResult waitSubscriptionResult(
    MarketDataResponseType responseType,
    List<Instrument> instruments
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
    getSubscriptionStatusFromResponse(response).ifPresent(this::processSubscriptionResult);
  }

  protected void processSubscriptionResult(MarketDataSubscriptionResult subscriptionResult) {
    try {
      getSubscriptionsMap(subscriptionResult.getResponseType())
        .putAll(subscriptionResult.getSubscriptionStatusMap().entrySet().stream()
          .filter(entry -> entry.getValue().isOk())
          .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()))
          .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue)));
      int prevSubscriptionsCount = getSubscriptionsCount();
      int subscriptionsCount = updateAndGetSubscriptionsCount();
      if (subscriptionsCount == 0 && healthCheckFutureRef.get() != null) {
        logger.debug("Healthcheck disabled");
        healthCheckFutureRef.get().cancel(true);
        healthCheckFutureRef.set(null);
      } else if (subscriptionsCount > 0) {
        if (prevSubscriptionsCount == 0) {
          sendPingSettings();
        }
        if (healthCheckFutureRef.get() == null) {
          logger.debug("Healthcheck enabled");
          healthCheckFutureRef.set(executorService.scheduleAtFixedRate(this::healthCheck, 0, pingDelay, TimeUnit.MILLISECONDS));
        }
      }
      subscriptionResultsQueue.put(subscriptionResult);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void sendPingSettings() {
    var pingRequest = MarketDataRequest.newBuilder()
      .setPingSettings(PingDelaySettings.newBuilder()
        .setPingDelayMs(pingDelay)
        .build())
      .build();
    streamWrapper.newCall(pingRequest);
  }

  protected Optional<MarketDataSubscriptionResult> getSubscriptionStatusFromResponse(MarketDataResponse response) {
    if (response.hasSubscribeCandlesResponse()) {
      return Optional.of(SubscriptionResultMapper.map(response.getSubscribeCandlesResponse()));
    } else if (response.hasSubscribeLastPriceResponse()) {
      return Optional.of(SubscriptionResultMapper.map(response.getSubscribeLastPriceResponse()));
    } else if (response.hasSubscribeOrderBookResponse()) {
      return Optional.of(SubscriptionResultMapper.map(response.getSubscribeOrderBookResponse()));
    } else if (response.hasSubscribeTradesResponse()) {
      return Optional.of(SubscriptionResultMapper.map(response.getSubscribeTradesResponse()));
    } else if (response.hasSubscribeInfoResponse()) {
      return Optional.of(SubscriptionResultMapper.map(response.getSubscribeInfoResponse()));
    }
    return Optional.empty();
  }

  protected void healthCheck() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastInteractionTime.get() > inactivityTimeout) {
      logger.info("Reconnecting...");
      disconnectWrapper();
      streamWrapper.connect();
      var future = CompletableFuture.completedFuture(null);
      requests.forEach(request -> future.thenRunAsync(() -> streamWrapper.newCall(request)));
      lastInteractionTime.set(System.currentTimeMillis());
    }
  }

  protected int updateAndGetSubscriptionsCount() {
    return subscriptionsCount.addAndGet(subscriptionsMap.values().stream().mapToInt(Map::size).sum());
  }

  private void disconnectWrapper() {
    subscriptionsCount.set(0);
    subscriptionsMap.values().forEach(Map::clear);
    streamWrapper.disconnect();
  }
}
