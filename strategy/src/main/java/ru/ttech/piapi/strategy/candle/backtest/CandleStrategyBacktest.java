package ru.ttech.piapi.strategy.candle.backtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.analysis.cost.ZeroCostModel;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.num.DecimalNum;
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
  private final BarSeries barSeries;

  public CandleStrategyBacktest(
    CandleStrategyBacktestConfiguration configuration,
    HistoryDataApiClient historyDataApiClient
  ) {
    this.configuration = configuration;
    this.historyDataApiClient = historyDataApiClient;
    this.historyCandleCsvReader = new HistoryCandleCsvReader();
    this.barSeries = new BaseBarSeriesBuilder().withNumTypeOf(DecimalNum.class).build();
  }

  public void run() {
    var bars = prepareBars();
    BarSeriesUtils.addBars(barSeries, aggregateBars(bars.listIterator()));
    logger.info("Backtest started...");
    var tradeFeeModel = configuration.getTradeFeeModel();
    var tradeExecutionModel = configuration.getTradeExecutionModel();
    var barSeriesManager = new BarSeriesManager(barSeries, tradeFeeModel, new ZeroCostModel(), tradeExecutionModel);
    configuration.getStrategyAnalysis().accept(barSeriesManager);
    logger.info("Backtest finished!");
  }

  private List<Bar> prepareBars() {
    int fromYear = configuration.getFrom().getYear();
    int toYear = configuration.getTo().getYear();
    var fromTime = configuration.getFrom().atStartOfDay(ZoneId.systemDefault());
    var toTime = configuration.getTo().atStartOfDay(ZoneId.systemDefault());
    var instrumentId = configuration.getInstrumentId();
    var bars = Collections.synchronizedList(new LinkedList<Bar>());

    logger.info("Start loading historical data...");
    IntStream.rangeClosed(fromYear, toYear)
      .mapToObj(year -> CompletableFuture.runAsync(() ->
        historyDataApiClient.downloadHistoricalDataArchive(instrumentId, year),
        configuration.getExecutorService()))
      .collect(Collectors.toList())
      .forEach(CompletableFuture::join);
    logger.info("Loading historical data finished!");
    logger.info("Start reading historical data...");
    IntStream.rangeClosed(fromYear, toYear).forEach(year -> {
      List<Bar> yearBars = historyCandleCsvReader.readHistoricalData(instrumentId, year, fromTime.toString(), toTime.toString());
      bars.addAll(yearBars);
    });
    logger.info("Reading historical data finished!");
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
