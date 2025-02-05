package ru.ttech.piapi.strategy.candle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNum;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.tinkoff.piapi.contract.v1.MarketDataServiceGrpc;
import ru.tinkoff.piapi.contract.v1.SubscribeCandlesRequest;
import ru.tinkoff.piapi.contract.v1.SubscriptionAction;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.resilience.ResilienceConfiguration;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.core.impl.marketdata.MarketDataStreamConfiguration;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class CandleStrategy {

  private static final Logger logger = LoggerFactory.getLogger(CandleStrategy.class);
  private final CandleStrategyConfiguration configuration;
  private final StreamServiceStubFactory streamFactory;
  private final ServiceStubFactory serviceFactory;
  private BarSeries barSeries;

  public CandleStrategy(CandleStrategyConfiguration configuration, StreamServiceStubFactory streamFactory) {
    this.serviceFactory = streamFactory.getServiceStubFactory();
    this.configuration = configuration;
    this.streamFactory = streamFactory;
  }

  public void run() {
    logger.info("Initializing candle strategy...");
    var bars = loadHistoricalCandles().stream()
      .map(this::convertHistoricCandleToBar)
      .collect(Collectors.toList());
    barSeries = new BaseBarSeriesBuilder().withNumTypeOf(DecimalNum.class)
      .withBars(bars)
      .build();

    var stream = streamFactory.newBidirectionalStream(
      MarketDataStreamConfiguration.builder()
        .addOnCandleListener(candle -> {
          barSeries.addBar(convertCandleToBar(candle));
          logger.info("New candle was added to bar series");
        })
        .build());
    stream.connect();
    stream.newCall(MarketDataRequest.newBuilder()
      .setSubscribeCandlesRequest(SubscribeCandlesRequest.newBuilder()
        .setSubscriptionAction(SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE)
        .addInstruments(configuration.getInstrument())
        .setWaitingClose(true)
        .setCandleSourceType(configuration.getCandleSource())
        .build())
      .build());
    logger.info("Candle strategy started");
  }

  private Bar convertCandleToBar(CandleWrapper candle) {
    var startTime = candle.getTime();
    var period = PeriodMapper.getTimePeriod(startTime, candle.getInterval());
    var endTime = startTime.plusMinutes(period.toMinutes()).atZone(ZoneOffset.UTC);
    return BaseBar.builder()
      .timePeriod(period)
      .endTime(endTime)
      .openPrice(DecimalNum.valueOf(candle.getOpen()))
      .closePrice(DecimalNum.valueOf(candle.getClose()))
      .lowPrice(DecimalNum.valueOf(candle.getLow()))
      .highPrice(DecimalNum.valueOf(candle.getHigh()))
      .volume(DecimalNum.valueOf(candle.getVolume()))
      .build();
  }

  private Bar convertHistoricCandleToBar(HistoricCandle candle) {
    var startTime = TimeMapper.timestampToLocalDateTime(candle.getTime());
    var interval = configuration.getInstrument().getInterval();
    var period = PeriodMapper.getTimePeriod(startTime, interval);
    var endTime = startTime.plusMinutes(period.toMinutes()).atZone(ZoneOffset.UTC);
    return BaseBar.builder()
      .timePeriod(period)
      .endTime(endTime)
      .openPrice(DecimalNum.valueOf(NumberMapper.quotationToBigDecimal(candle.getOpen())))
      .closePrice(DecimalNum.valueOf(NumberMapper.quotationToBigDecimal(candle.getClose())))
      .lowPrice(DecimalNum.valueOf(NumberMapper.quotationToBigDecimal(candle.getLow())))
      .highPrice(DecimalNum.valueOf(NumberMapper.quotationToBigDecimal(candle.getHigh())))
      .volume(DecimalNum.valueOf(candle.getVolume()))
      .build();
  }

  private List<HistoricCandle> loadHistoricalCandles() {
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
