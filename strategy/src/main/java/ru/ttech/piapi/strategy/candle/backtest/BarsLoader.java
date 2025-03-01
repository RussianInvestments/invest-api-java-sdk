package ru.ttech.piapi.strategy.candle.backtest;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
  private static final CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.RFC4180)
    .setHeader("start_time", "open", "high", "low", "close", "volume")
    .setSkipHeaderRecord(true)
    .get();
  private final HistoryDataApiClient historyDataApiClient;
  private final HistoryCandleCsvReader historyCandleCsvReader;
  private final ExecutorService executorService;

  public BarsLoader(
    ConnectorConfiguration connectorConfiguration,
    ExecutorService executorService
  ) {
    this.historyDataApiClient = new HistoryDataApiClient(connectorConfiguration);
    this.historyCandleCsvReader = new HistoryCandleCsvReader();
    this.executorService = executorService;
  }

  public Iterable<BarData> loadBars(String instrumentId, CandleInterval candleInterval, LocalDate from) {
    return loadBars(instrumentId, candleInterval, from, LocalDate.now());
  }

  public Iterable<BarData> loadBars(String instrumentId, CandleInterval candleInterval, LocalDate from, LocalDate to) {
    var barsData = loadMinuteBars(instrumentId, from, to);
    return aggregateBars(barsData, candleInterval);
  }

  public void saveBars(Path outputFile, Iterable<BarData> bars) {
    if (Files.exists(outputFile)) {
      logger.warn("File {} already exists", outputFile);
      return;
    }
    try (var writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
         var printer = new CSVPrinter(writer, csvFormat)) {
      printer.printRecord((Object[]) csvFormat.getHeader());
      bars.forEach(bar -> {
        try {
          printer.printRecord(bar.toArray());
        } catch (IOException e) {
          logger.error("Error occurred while printing bar: {}", bar);
        }
      });
    } catch (IOException e) {
      logger.error("Error while writing file {}!", outputFile);
    }
  }

  private Iterator<BarData> loadMinuteBars(String instrumentId, LocalDate from, LocalDate to) {
    logger.debug("Start loading historical data for instrument {} ...", instrumentId);
    CompletableFuture.allOf(IntStream.rangeClosed(from.getYear(), to.getYear())
      .mapToObj(year -> CompletableFuture.runAsync(() ->
        historyDataApiClient.downloadHistoricalDataArchive(instrumentId, year), executorService))
      .toArray(CompletableFuture[]::new)).join();
    logger.debug("Loading historical data finished for instrument {}!", instrumentId);

    logger.debug("Start reading historical data for instrument {}...", instrumentId);
    return new Iterator<>() {
      private final Iterator<Integer> yearsIterator = IntStream.rangeClosed(from.getYear(), to.getYear()).iterator();
      private Iterator<BarData> currentYearIterator = Collections.emptyIterator();
      private boolean readingFinished = false;

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
          if (!readingFinished) {
            logger.info("Reading historical data for instrument {} finished!", instrumentId);
            readingFinished = true;
          }
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
