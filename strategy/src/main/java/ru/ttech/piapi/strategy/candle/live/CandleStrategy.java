package ru.ttech.piapi.strategy.candle.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.utils.BarSeriesUtils;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataServiceGrpc;
import ru.tinkoff.piapi.contract.v1.SubscribeCandlesRequest;
import ru.tinkoff.piapi.contract.v1.SubscriptionAction;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.resilience.ResilienceAsyncStubWrapper;
import ru.ttech.piapi.core.connector.resilience.ResilienceConfiguration;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.core.impl.marketdata.MarketDataStreamManager;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;
import ru.ttech.piapi.strategy.candle.mapper.BarMapper;
import ru.ttech.piapi.strategy.candle.mapper.PeriodMapper;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class CandleStrategy {

  private static final Logger logger = LoggerFactory.getLogger(CandleStrategy.class);

  private final CandleStrategyConfiguration configuration;
  private final ServiceStubFactory serviceFactory;
  private final MarketDataStreamManager streamManager;

  public CandleStrategy(
    CandleStrategyConfiguration configuration,
    MarketDataStreamManager streamManager
  ) {
    this.streamManager = streamManager;
    this.configuration = configuration;
    this.serviceFactory = streamManager.getStreamFactory().getServiceStubFactory();
  }

  public void run() {
    logger.info("Initializing candle strategy...");
    var executorService = Executors.newSingleThreadScheduledExecutor();
    var connectorConfiguration = serviceFactory.getConfiguration();
    var marketDataService = serviceFactory.newResilienceAsyncService(
      MarketDataServiceGrpc::newStub,
      ResilienceConfiguration.builder(executorService, connectorConfiguration).build()
    );
    var futures = configuration.getBarSeriesMap().entrySet().stream().map(entry ->
      downloadHistoricalCandles(marketDataService, entry.getKey())
        .thenAcceptAsync(historicCandles -> {
          var bars = historicCandles.stream()
            .map(candle -> BarMapper.convertHistoricCandleToBar(candle, entry.getKey().getInterval()))
            .collect(Collectors.toList());
          BarSeriesUtils.addBars(entry.getValue(), bars);
        })
        .thenAcceptAsync(__ -> {
          streamManager.subscribe(MarketDataRequest.newBuilder()
            .setSubscribeCandlesRequest(SubscribeCandlesRequest.newBuilder()
              .setSubscriptionAction(SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE)
              .addInstruments(entry.getKey())
              .setWaitingClose(true)
              .setCandleSourceType(configuration.getCandleSource())
              .build())
            .build());
          logger.info("Subscribed for candles for instrument {}", entry.getKey().getInstrumentId());
        })).toArray(CompletableFuture[]::new);
    CompletableFuture.allOf(futures).whenCompleteAsync((__, throwable) -> {
      executorService.shutdown();
      streamManager.addOnCandleListener(this::proceedNewCandle);
      logger.info("Executor shutdown");
      logger.info("Candle strategy started");
    });
  }

  private void proceedNewCandle(CandleWrapper candle) {
    var foundInstrument = configuration.getBarSeriesMap().keySet().stream()
      .filter(instrument ->
        instrument.getInstrumentId().equals(candle.getInstrumentUid())
          && instrument.getInterval() == candle.getInterval())
      .findAny();
    if (foundInstrument.isEmpty()) {
      return;
    }
    logger.info("New candle received! for series {}", foundInstrument.get().getInstrumentId());
    try {
      var barSeries = configuration.getBarSeriesMap().get(foundInstrument.get());
      barSeries.addBar(BarMapper.convertCandleWrapperToBar(candle));
      int endIndex = barSeries.getEndIndex();
      var strategy = configuration.getStrategiesMap().get(foundInstrument.get());
      if (strategy.shouldEnter(endIndex)) {
        configuration.getEnterAction().accept(foundInstrument.get(), barSeries.getBar(endIndex));
      } else if (strategy.shouldExit(endIndex)) {
        configuration.getExitAction().accept(foundInstrument.get(), barSeries.getBar(endIndex));
      }
      logger.info("New candle was added to bar series");
    } catch (IllegalArgumentException e) {
      logger.warn("Bar already was added");
    }
  }

  private CompletableFuture<List<HistoricCandle>> downloadHistoricalCandles(
    ResilienceAsyncStubWrapper<MarketDataServiceGrpc.MarketDataServiceStub> marketDataService,
    CandleInstrument instrument
  ) {
    logger.info("Loading warmup bars for instrument {}...", instrument.getInstrumentId());
    var endTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    var interval = instrument.getInterval();
    var candlesResponse = marketDataService.callAsyncMethod(
      MarketDataServiceGrpc.getGetCandlesMethod(),
      (stub, observer) -> stub.getCandles(
        GetCandlesRequest.newBuilder()
          .setFrom(TimeMapper.localDateTimeToTimestamp(PeriodMapper.getStartTime(endTime, interval)))
          .setTo(TimeMapper.localDateTimeToTimestamp(endTime))
          .setInstrumentId(instrument.getInstrumentId())
          .setInterval(CandleInterval.forNumber(interval.getNumber()))
          .build(), observer));
    return candlesResponse.thenApplyAsync(response -> {
      logger.info("Warmup bars loaded");
      var candlesList = response.getCandlesList();
      int candlesLength = Math.max(candlesList.size() - configuration.getWarmupLength(), 0);
      return candlesList.subList(candlesLength, candlesList.size());
    });
  }
}
