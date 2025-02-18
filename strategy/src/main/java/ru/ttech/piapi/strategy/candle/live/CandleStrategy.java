package ru.ttech.piapi.strategy.candle.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.utils.BarSeriesUtils;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.MarketDataServiceGrpc;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.resilience.ResilienceAsyncStubWrapper;
import ru.ttech.piapi.core.connector.resilience.ResilienceConfiguration;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.core.impl.marketdata.MarketDataStreamManager;
import ru.ttech.piapi.core.impl.marketdata.subscription.CandleSubscriptionSpec;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;
import ru.ttech.piapi.strategy.candle.mapper.BarMapper;
import ru.ttech.piapi.strategy.candle.mapper.PeriodMapper;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Стратегия на основе японских свечей
 */
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

  /**
   * Метод для запуска стратегии.
   * После вызова происходит загрузка исторических свечей для всех инструментов,
   * подписка на новые свечи в стримах и установка действий при входе и выходе по стратегиям
   */
  public void run() {
    logger.info("Initializing candle strategy...");
    produceWarmupAsync().whenCompleteAsync((unused, throwable) -> {
      var instruments = configuration.getBarSeriesMap().keySet().stream()
        .map(candleInstrument -> new Instrument(candleInstrument.getInstrumentId(), candleInstrument.getInterval()))
        .collect(Collectors.toList());
      logger.info("Subscribing to candles...");
      streamManager.subscribeCandles(
        instruments,
        new CandleSubscriptionSpec(configuration.getCandleSource()),
        this::proceedNewCandle
      );
      streamManager.start();
      logger.info("Candle strategy started");
    });
  }

  private void proceedNewCandle(CandleWrapper candle) {
    var candleInstrument = findCandleInstrument(candle);
    if (candleInstrument.isEmpty()) {
      return;
    }
    logger.info("New candle received! for series {}", candleInstrument.get().getInstrumentId());
    try {
      var barSeries = configuration.getBarSeriesMap().get(candleInstrument.get());
      barSeries.addBar(BarMapper.convertCandleWrapperToBar(candle));
      int endIndex = barSeries.getEndIndex();
      var strategy = configuration.getStrategiesMap().get(candleInstrument.get());
      if (strategy.shouldEnter(endIndex)) {
        configuration.getEnterAction().accept(candleInstrument.get(), barSeries.getBar(endIndex));
      } else if (strategy.shouldExit(endIndex)) {
        configuration.getExitAction().accept(candleInstrument.get(), barSeries.getBar(endIndex));
      }
      logger.info("New candle was added to bar series");
    } catch (IllegalArgumentException e) {
      logger.warn("Bar already was added");
    }
  }

  private Optional<CandleInstrument> findCandleInstrument(CandleWrapper candle) {
    return configuration.getBarSeriesMap().keySet().stream()
      .filter(instrument ->
        instrument.getInstrumentId().equals(candle.getInstrumentUid())
          && instrument.getInterval() == candle.getInterval())
      .findAny();
  }

  private CompletableFuture<Void> produceWarmupAsync() {
    var executorService = Executors.newSingleThreadScheduledExecutor();
    var connectorConfiguration = serviceFactory.getConfiguration();
    var marketDataService = serviceFactory.newResilienceAsyncService(
      MarketDataServiceGrpc::newStub,
      ResilienceConfiguration.builder(executorService, connectorConfiguration).build()
    );
    logger.info("Downloading historical candles...");
    var futures = configuration.getBarSeriesMap().entrySet().stream().map(entry ->
        downloadHistoricalCandlesAsync(marketDataService, entry.getKey())
          .thenAcceptAsync(historicCandles -> {
            var bars = historicCandles.stream()
              .map(candle -> BarMapper.convertHistoricCandleToBar(candle, entry.getKey().getInterval()))
              .collect(Collectors.toList());
            BarSeriesUtils.addBars(entry.getValue(), bars);
          }))
      .toArray(CompletableFuture[]::new);
    return CompletableFuture.allOf(futures).whenCompleteAsync((unused, throwable) -> {
      executorService.shutdown();
      logger.info("Historical candles successfully downloaded!");
      logger.debug("Historical bars loader executor service was shutdown");
    });
  }

  private CompletableFuture<List<HistoricCandle>> downloadHistoricalCandlesAsync(
    ResilienceAsyncStubWrapper<MarketDataServiceGrpc.MarketDataServiceStub> marketDataService,
    CandleInstrument instrument
  ) {
    logger.debug("Loading warmup bars for instrument {}...", instrument.getInstrumentId());
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
      logger.debug("Warmup bars for instrument {} were load!", instrument.getInstrumentId());
      var candlesList = response.getCandlesList().stream()
        .filter(HistoricCandle::getIsComplete)
        .collect(Collectors.toList());
      int candlesLength = Math.max(candlesList.size() - configuration.getWarmupLength(), 0);
      return candlesList.subList(candlesLength, candlesList.size());
    });
  }
}
