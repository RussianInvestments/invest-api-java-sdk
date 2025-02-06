package ru.ttech.piapi.strategy.candle.backtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.utils.BarSeriesUtils;
import ru.ttech.piapi.strategy.candle.mapper.PeriodMapper;

import java.time.ZoneId;
import java.util.Collections;
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
    var instrument = configuration.getInstrument();
    var bars = prepareBars();
    var period = PeriodMapper.getTimePeriod(instrument.getInterval());
    var barSeries = configuration.getBarSeries();
    var tempBarSeries = new BaseBarSeriesBuilder()
      .withNumTypeOf(DecimalNum.class)
      .withBars(bars)
      .build();
    // TODO: заменить на самописную агрегацию баров, встроенная работает неверно
    var aggregatedBarSeries = BarSeriesUtils.aggregateBars(tempBarSeries, period, "new");
    BarSeriesUtils.addBars(barSeries, aggregatedBarSeries.getBarData());
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
    var instrumentId = configuration.getInstrument().getInstrumentId();
    var bars = Collections.synchronizedList(new LinkedList<Bar>());
    logger.debug("Start loading historical data...");
    List<CompletableFuture<Void>> futures = IntStream.rangeClosed(fromYear, toYear)
      .mapToObj(year -> CompletableFuture.runAsync(() -> {
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
}
