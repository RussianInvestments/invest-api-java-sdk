package ru.ttech.piapi.strategy.candle.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.utils.BarSeriesUtils;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.contract.v1.MarketDataServiceGrpc;
import ru.tinkoff.piapi.contract.v1.MarketDataStreamServiceGrpc;
import ru.tinkoff.piapi.contract.v1.SubscribeCandlesRequest;
import ru.tinkoff.piapi.contract.v1.SubscriptionAction;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.resilience.ResilienceConfiguration;
import ru.ttech.piapi.core.connector.streaming.BidirectionalStreamWrapper;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.core.impl.marketdata.MarketDataStreamConfiguration;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;
import ru.ttech.piapi.strategy.candle.mapper.BarMapper;
import ru.ttech.piapi.strategy.candle.mapper.PeriodMapper;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class CandleStrategy {

  private static final Logger logger = LoggerFactory.getLogger(CandleStrategy.class);
  private final CandleStrategyConfiguration configuration;
  private final StreamServiceStubFactory streamFactory;
  private final ServiceStubFactory serviceFactory;
  private BidirectionalStreamWrapper<
    MarketDataStreamServiceGrpc.MarketDataStreamServiceStub,
    MarketDataRequest,
    MarketDataResponse> stream;

  public CandleStrategy(CandleStrategyConfiguration configuration, StreamServiceStubFactory streamFactory) {
    this.serviceFactory = streamFactory.getServiceStubFactory();
    this.configuration = configuration;
    this.streamFactory = streamFactory;
  }

  public void run() {
    logger.info("Initializing candle strategy...");
    var instrument = configuration.getInstrument();
    var bars = downloadHistoricalCandles().stream()
      .map(candle -> BarMapper.convertHistoricCandleToBar(candle, instrument.getInterval()))
      .collect(Collectors.toList());
    BarSeriesUtils.addBars(configuration.getBarSeries(), bars);

    stream = streamFactory.newBidirectionalStream(
      MarketDataStreamConfiguration.builder()
        .addOnCandleListener(this::proceedNewCandle)
        .build());
    stream.connect();
    stream.newCall(MarketDataRequest.newBuilder()
      .setSubscribeCandlesRequest(SubscribeCandlesRequest.newBuilder()
        .setSubscriptionAction(SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE)
        .addInstruments(instrument)
        .setWaitingClose(true)
        .setCandleSourceType(configuration.getCandleSource())
        .build())
      .build());
    logger.info("Candle strategy started");
  }

  public void shutdown() {
    stream.disconnect();
  }

  private void proceedNewCandle(CandleWrapper candle) {
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

  private List<HistoricCandle> downloadHistoricalCandles() {
    logger.info("Loading warmup bars...");
    var executorService = Executors.newSingleThreadScheduledExecutor();
    var connectorConfiguration = serviceFactory.getConfiguration();
    var marketDataService = serviceFactory.newResilienceSyncService(
      MarketDataServiceGrpc::newBlockingStub,
      ResilienceConfiguration.builder(executorService, connectorConfiguration).build()
    );
    var endTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    var interval = configuration.getInstrument().getInterval();
    var candlesList = marketDataService.callSyncMethod(
        MarketDataServiceGrpc.getGetCandlesMethod(),
        stub -> stub.getCandles(
          GetCandlesRequest.newBuilder()
            .setFrom(TimeMapper.localDateTimeToTimestamp(PeriodMapper.getStartTime(endTime, interval)))
            .setTo(TimeMapper.localDateTimeToTimestamp(endTime))
            .setInstrumentId(configuration.getInstrument().getInstrumentId())
            .setInterval(CandleInterval.forNumber(interval.getNumber()))
            .build()))
      .getCandlesList();
    int candlesLength = Math.max(candlesList.size() - configuration.getWarmupLength(), 0);
    return candlesList.subList(candlesLength, candlesList.size());
  }
}
