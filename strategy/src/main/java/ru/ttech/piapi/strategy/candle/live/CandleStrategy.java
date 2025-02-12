package ru.ttech.piapi.strategy.candle.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.utils.BarSeriesUtils;
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

  public CandleStrategy(CandleStrategyConfiguration configuration, MarketDataStreamManager streamManager) {
    this.serviceFactory = streamManager.getStreamFactory().getServiceStubFactory();
    this.configuration = configuration;
    this.streamManager = streamManager;
  }

  public void run() {
    logger.info("Initializing candle strategy...");
    var instrument = configuration.getInstrument();
    var executorService = Executors.newSingleThreadScheduledExecutor();
    var connectorConfiguration = serviceFactory.getConfiguration();
    var marketDataService = serviceFactory.newResilienceAsyncService(
      MarketDataServiceGrpc::newStub,
      ResilienceConfiguration.builder(executorService, connectorConfiguration).build()
    );
    downloadHistoricalCandles(marketDataService).thenAcceptAsync(historicCandles -> {
      var bars = historicCandles.stream()
        .filter(HistoricCandle::getIsComplete)
        .map(candle -> BarMapper.convertHistoricCandleToBar(candle, instrument.getInterval()))
        .collect(Collectors.toList());
      BarSeriesUtils.addBars(configuration.getBarSeries(), bars);
      streamManager.addOnCandleListener(this::proceedNewCandle);
      streamManager.subscribe(MarketDataRequest.newBuilder()
        .setSubscribeCandlesRequest(SubscribeCandlesRequest.newBuilder()
          .setSubscriptionAction(SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE)
          .addInstruments(instrument)
          .setWaitingClose(true)
          .setCandleSourceType(configuration.getCandleSource())
          .build())
        .build());
    }).whenCompleteAsync((historicCandles, throwable) -> {
      executorService.shutdown();
      logger.info("Executor shutdown");
      logger.info("Candle strategy started");
    });
  }

  private void proceedNewCandle(CandleWrapper candle) {
    if (!candle.getInstrumentUid().equals(configuration.getInstrument().getInstrumentId())
      || candle.getInterval() != configuration.getInstrument().getInterval()) {
      return;
    }
    logger.info("New candle received! for series {}", configuration.getInstrument().getInstrumentId());
    try {
      var barSeries = configuration.getBarSeries();
      barSeries.addBar(BarMapper.convertCandleWrapperToBar(candle));
      int endIndex = barSeries.getEndIndex();
      var strategy = configuration.getStrategy();
      if (strategy.shouldEnter(endIndex)) {
        configuration.getEnterAction().accept(barSeries.getBar(endIndex));
      } else if (strategy.shouldExit(endIndex)) {
        configuration.getExitAction().accept(barSeries.getBar(endIndex));
      }
      logger.info("New candle was added to bar series");
    } catch (IllegalArgumentException e) {
      logger.warn("Bar already was added");
    }
  }

  private CompletableFuture<List<HistoricCandle>> downloadHistoricalCandles(
    ResilienceAsyncStubWrapper<MarketDataServiceGrpc.MarketDataServiceStub> marketDataService
    ) {
    logger.info("Loading warmup bars...");
    var endTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    var interval = configuration.getInstrument().getInterval();
    var candlesResponse = marketDataService.callAsyncMethod(
        MarketDataServiceGrpc.getGetCandlesMethod(),
        (stub, observer) -> stub.getCandles(
          GetCandlesRequest.newBuilder()
            .setFrom(TimeMapper.localDateTimeToTimestamp(PeriodMapper.getStartTime(endTime, interval)))
            .setTo(TimeMapper.localDateTimeToTimestamp(endTime))
            .setInstrumentId(configuration.getInstrument().getInstrumentId())
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
