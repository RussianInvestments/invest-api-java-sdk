package ru.ttech.piapi.strategy.candle.backtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.analysis.cost.ZeroCostModel;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.num.DoubleNum;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
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
    BarSeries barSeries = new BaseBarSeriesBuilder().withNumTypeOf(DoubleNum.class)
      .withBars(aggregateBars(bars))
      .build();
    logger.info("Backtest started...");
    var tradeFeeModel = configuration.getTradeFeeModel();
    var tradeExecutionModel = configuration.getTradeExecutionModel();
    var barSeriesManager = new BarSeriesManager(barSeries, tradeFeeModel, new ZeroCostModel(), tradeExecutionModel);
    configuration.getStrategyAnalysis().accept(barSeriesManager);
    logger.info("Backtest finished!");
  }

  private Iterator<BarData> prepareBars() {
    int fromYear = configuration.getFrom().getYear();
    int toYear = configuration.getTo().getYear();
    var instrumentId = configuration.getInstrumentId();
    logger.info("Start loading historical data...");
    CompletableFuture.allOf(IntStream.rangeClosed(fromYear, toYear)
      .mapToObj(year -> CompletableFuture.runAsync(() ->
          historyDataApiClient.downloadHistoricalDataArchive(instrumentId, year),
        configuration.getExecutorService()))
      .toArray(CompletableFuture[]::new)).join();
    logger.info("Loading historical data finished!");

    logger.info("Start reading historical data...");
    return new Iterator<>() {
      private final Iterator<Integer> yearsIterator = IntStream.rangeClosed(fromYear, toYear).iterator();
      private Iterator<BarData> currentYearIterator = Collections.emptyIterator();

      @Override
      public boolean hasNext() {
        while (!currentYearIterator.hasNext() && yearsIterator.hasNext()) {
          int year = yearsIterator.next();
          currentYearIterator = historyCandleCsvReader.readHistoricalData(
            instrumentId,
            year,
            DateTimeFormatter.ISO_DATE.format(configuration.getFrom()),
            DateTimeFormatter.ISO_DATE.format(configuration.getTo())
          );
        }

        if (!currentYearIterator.hasNext()) {
          logger.info("Reading historical data finished!");
        }

        return currentYearIterator.hasNext();
      }

      @Override
      public BarData next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return currentYearIterator.next();
      }
    };
  }

  private List<Bar> aggregateBars(Iterator<BarData> bars) {
    List<Bar> aggregatedBars = new LinkedList<>();
    BarData currentBar = null;
    while (bars.hasNext() || currentBar != null) {
      BarData bar = currentBar != null ? currentBar : bars.next();
      currentBar = null;
      ZonedDateTime startTime = TimeHelper.roundFloorStartTime(
        ZonedDateTime.parse(bar.getStartTime()),
        configuration.getCandleInterval());
      ZonedDateTime endTime = TimeHelper.getEndTime(startTime, configuration.getCandleInterval());
      String endTimeStr = DateTimeFormatter.ISO_DATE_TIME.format(endTime);
      var high = bar.getHigh();
      var low = bar.getLow();
      var close = bar.getClose();
      var volume = bar.getVolume();
      while (bars.hasNext()) {
        var nextBar = bars.next();
        if (nextBar.getStartTime().compareTo(endTimeStr) >= 0) {
          currentBar = nextBar;
          break;
        }
        high = Math.max(nextBar.getHigh(), high);
        low = Math.min(nextBar.getLow(), low);
        close = nextBar.getClose();
        volume += nextBar.getVolume();
      }
      var aggregatedBar = BaseBar.builder()
        .endTime(endTime)
        .timePeriod(Duration.between(startTime, endTime))
        .openPrice(DoubleNum.valueOf(bar.getOpen()))
        .highPrice(DoubleNum.valueOf(high))
        .lowPrice(DoubleNum.valueOf(low))
        .closePrice(DoubleNum.valueOf(close))
        .volume(DoubleNum.valueOf(volume))
        .build();
      aggregatedBars.add(aggregatedBar);
    }
    return aggregatedBars;
  }
}
