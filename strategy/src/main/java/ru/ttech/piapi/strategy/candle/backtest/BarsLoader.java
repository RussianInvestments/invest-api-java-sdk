package ru.ttech.piapi.strategy.candle.backtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.CandleInterval;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

public class BarsLoader {

  private static final Logger logger = LoggerFactory.getLogger(BarsLoader.class);
  private final HistoryDataApiClient historyDataApiClient;
  private final HistoryCandleCsvReader historyCandleCsvReader;
  private final ExecutorService executorService;

  public BarsLoader(
    HistoryDataApiClient historyDataApiClient,
    HistoryCandleCsvReader historyCandleCsvReader,
    ExecutorService executorService
  ) {
    this.historyDataApiClient = historyDataApiClient;
    this.historyCandleCsvReader = historyCandleCsvReader;
    this.executorService = executorService;
  }

  public Iterable<BarData> loadBars(String instrumentId, CandleInterval candleInterval, LocalDate from, LocalDate to) {
    var barsData = prepareBars(instrumentId, from, to);
    return aggregateBars(barsData, candleInterval);
  }

  private Iterator<BarData> prepareBars(String instrumentId, LocalDate from, LocalDate to) {
    logger.info("Start loading historical data...");
    CompletableFuture.allOf(IntStream.rangeClosed(from.getYear(), to.getYear())
      .mapToObj(year -> CompletableFuture.runAsync(() ->
          historyDataApiClient.downloadHistoricalDataArchive(instrumentId, year), executorService))
      .toArray(CompletableFuture[]::new)).join();
    logger.info("Loading historical data finished!");

    logger.info("Start reading historical data...");
    return new Iterator<>() {
      private final Iterator<Integer> yearsIterator = IntStream.rangeClosed(from.getYear(), to.getYear()).iterator();
      private Iterator<BarData> currentYearIterator = Collections.emptyIterator();

      @Override
      public boolean hasNext() {
        while (!currentYearIterator.hasNext() && yearsIterator.hasNext()) {
          int year = yearsIterator.next();
          currentYearIterator = historyCandleCsvReader.readHistoricalData(
            instrumentId,
            year,
            DateTimeFormatter.ISO_DATE.format(from),
            DateTimeFormatter.ISO_DATE.format(to)
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

  private Iterable<BarData> aggregateBars(Iterator<BarData> bars, CandleInterval interval) {
    return () -> new Iterator<>() {
      private BarData currentBar = null;

      @Override
      public boolean hasNext() {
        return bars.hasNext() || currentBar != null;
      }

      @Override
      public BarData next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }

        BarData bar = currentBar != null ? currentBar : bars.next();
        currentBar = null;
        var startTime = TimeHelper.roundFloorStartTime(ZonedDateTime.parse(bar.getStartTime()), interval);
        var endTime = TimeHelper.getEndTime(startTime, interval);
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

        return new BarData(startTime.format(DateTimeFormatter.ISO_DATE_TIME), bar.getOpen(), high, low, close, volume);
      }
    };
  }
}
