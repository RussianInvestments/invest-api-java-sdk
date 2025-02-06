package ru.ttech.piapi.strategy.candle.backtest;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.num.DecimalNum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;

public class HistoryCandleCsvReader {

  private static final Logger log = LoggerFactory.getLogger(HistoryCandleCsvReader.class);
  private static final String FILENAME_PATTERN = "%s_%d.zip";

  public List<Bar> readHistoricalData(String instrumentUid, int year) {
    List<Bar> bars = new LinkedList<>();
    String fileName = String.format(FILENAME_PATTERN, instrumentUid, year);
    try (ZipFile zip = ZipFile.builder().setFile(fileName).get()) {
      Collections.list(zip.getEntries()).stream()
        .sorted(Comparator.comparing(ZipEntry::getName))
        .forEach(entry -> addBarsFromEntryToList(zip, entry, bars));
    } catch (IOException e) {
      log.error("Error occurred while reading file: {}", fileName);
    }
    return bars;
  }

  private void addBarsFromEntryToList(ZipFile zip, ZipArchiveEntry entry, List<Bar> bars) {
    if (!entry.isDirectory()) {
      String fileName = entry.getName();
      try (InputStream inputStream = zip.getInputStream(entry);
           BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
        String line;
        while ((line = reader.readLine()) != null) {
          bars.add(parseBarDataFromLine(line));
        }
      } catch (IOException e) {
        log.error("Error occurred while reading zip data: {}", e.getMessage());
      }
      log.trace("Successfully fetched zip entry! Filename: {}", fileName);
    }
  }

  private Bar parseBarDataFromLine(String line) {
    String[] data = line.split(";");
    return BaseBar.builder()
      .endTime(ZonedDateTime.parse(data[1]))
      .timePeriod(Duration.ofMinutes(1))
      .openPrice(DecimalNum.valueOf(data[2]))
      .highPrice(DecimalNum.valueOf(data[4]))
      .lowPrice(DecimalNum.valueOf(data[5]))
      .closePrice(DecimalNum.valueOf(data[3]))
      .volume(DecimalNum.valueOf(data[6]))
      .build();
  }
}
