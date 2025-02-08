package ru.ttech.piapi.strategy.candle.backtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.utils.BarSeriesUtils;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CandleStrategyBacktest {

  private static final Logger logger = LoggerFactory.getLogger(CandleStrategyBacktest.class);
  private final CandleStrategyBacktestConfiguration configuration;
  private final HistoryDataApiClient historyDataApiClient;
  private final HistoryCandleCsvReader historyCandleCsvReader;

  public CandleStrategyBacktest(
    CandleStrategyBacktestConfiguration configuration,
    HistoryDataApiClient historyDataApiClient
  ) {
    this.configuration = configuration;
    this.historyDataApiClient = historyDataApiClient;
    this.historyCandleCsvReader = new HistoryCandleCsvReader();
  }

  public void run() {
    var bars = prepareBars();
    // т.к. порядок не гарантирован
    BarSeriesUtils.sortBars(bars);
    // TODO: только если интервал больше минуты
    var aggregatedBars = aggregateBars(bars.listIterator());
    var barSeries = configuration.getBarSeries();
    BarSeriesUtils.addBars(barSeries, aggregatedBars);
    logger.info("Backtest started...");
    var tradingRecord = new BarSeriesManager(barSeries).run(configuration.getStrategy());
    configuration.getStrategyAnalysis().accept(barSeries, tradingRecord);
    logger.info("Backtest finished!");
  }

  private List<Bar> prepareBars() {
    int fromYear = configuration.getFrom().getYear();
    int toYear = configuration.getTo().getYear();
    var fromTime = configuration.getFrom().atStartOfDay(ZoneId.systemDefault());
    var toTime = configuration.getTo().atStartOfDay(ZoneId.systemDefault());
    var instrumentId = configuration.getInstrumentId();
    var bars = Collections.synchronizedList(new LinkedList<Bar>());
    logger.debug("Start loading historical data...");
    List<CompletableFuture<Void>> futures = IntStream.rangeClosed(fromYear, toYear)
      .mapToObj(year -> CompletableFuture.runAsync(() -> {
        // TODO: поместить в пул потоков
        historyDataApiClient.downloadHistoricalDataArchive(instrumentId, year);
        List<Bar> yearBars = historyCandleCsvReader.readHistoricalData(instrumentId, year).stream()
          .filter(bar -> bar.getEndTime().isAfter(fromTime) && bar.getEndTime().isBefore(toTime))
          .collect(Collectors.toList());
        bars.addAll(yearBars);
      }))
      .collect(Collectors.toList());
    futures.forEach(CompletableFuture::join);
    logger.debug("Loading historical data finished!");
    return bars;
  }

  private List<Bar> aggregateBars(Iterator<Bar> bars) {
    List<Bar> aggregatedBars = new LinkedList<>();
    Bar currentBar = null;
    while (bars.hasNext() || currentBar != null) {
      Bar bar = currentBar != null ? currentBar : bars.next();
      currentBar = null;
      ZonedDateTime startTime = TimeHelper.roundFloorStartTime(bar.getBeginTime(), configuration.getCandleInterval());
      ZonedDateTime endTime = TimeHelper.getEndTime(startTime, configuration.getCandleInterval());
      var highPrice = bar.getHighPrice();
      var lowPrice = bar.getLowPrice();
      var closePrice = bar.getClosePrice();
      var volume = bar.getVolume();
      while (bars.hasNext()) {
        var nextBar = bars.next();
        if (nextBar.getBeginTime().isEqual(endTime) || nextBar.getBeginTime().isAfter(endTime)) {
          currentBar = nextBar;
          break;
        }
        highPrice = nextBar.getHighPrice().isGreaterThan(highPrice)
          ? nextBar.getHighPrice()
          : highPrice;
        lowPrice = nextBar.getLowPrice().isLessThan(lowPrice)
          ? nextBar.getLowPrice()
          : lowPrice;
        closePrice = nextBar.getClosePrice();
        volume = nextBar.getVolume().plus(volume);
      }
      var aggregatedBar = BaseBar.builder()
        .endTime(endTime)
        .timePeriod(Duration.between(startTime, endTime))
        .openPrice(bar.getOpenPrice())
        .highPrice(highPrice)
        .lowPrice(lowPrice)
        .closePrice(closePrice)
        .volume(volume)
        .build();
      aggregatedBars.add(aggregatedBar);
    }
    return aggregatedBars;
  }
}
