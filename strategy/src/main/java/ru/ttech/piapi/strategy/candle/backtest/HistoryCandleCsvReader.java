package ru.ttech.piapi.strategy.candle.backtest;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class HistoryCandleCsvReader {

  private static final Logger log = LoggerFactory.getLogger(HistoryCandleCsvReader.class);
  private static final String FILENAME_PATTERN = "%s_%d.zip";
  private final String candlesDownloadPath;

  public HistoryCandleCsvReader(String candlesDownloadPath) {
    this.candlesDownloadPath = candlesDownloadPath;
  }

  public Iterator<BarData> readHistoricalData(String instrumentUid, int year, String startDate, String endDate) {
    String fileName = String.format(FILENAME_PATTERN, instrumentUid, year);
    if (candlesDownloadPath != null && !candlesDownloadPath.isBlank()) {
      fileName = candlesDownloadPath + fileName;
    }
    try {
      ZipFile zip = ZipFile.builder().setFile(fileName).get();
      List<ZipEntry> sortedEntries = Collections.list(zip.getEntries()).stream()
        .sorted(Comparator.comparing(ZipEntry::getName))
        .collect(Collectors.toList());

      return new Iterator<>() {

        private final Iterator<ZipEntry> entriesIterator = sortedEntries.iterator();
        private Iterator<BarData> currentEntryIterator = Collections.emptyIterator();
        private boolean zipClosed = false;

        @Override
        public boolean hasNext() {
          while (!currentEntryIterator.hasNext() && entriesIterator.hasNext()) {
            currentEntryIterator = getBarsFromEntry(zip,
              (ZipArchiveEntry) entriesIterator.next(),
              startDate,
              endDate);
          }

          if (!currentEntryIterator.hasNext() && !zipClosed) {
            try {
              zip.close();
              zipClosed = true;
            } catch (IOException e) {
              log.error("Error closing zip file: {}", e.getMessage());
            }
          }

          return currentEntryIterator.hasNext();
        }

        @Override
        public BarData next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          return currentEntryIterator.next();
        }
      };
    } catch (IOException e) {
      log.error("Error occurred while reading file: {}", fileName);
      return Collections.emptyIterator();
    }
  }

  private Iterator<BarData> getBarsFromEntry(ZipFile zip, ZipArchiveEntry entry, String startDate, String endDate) {
    if (entry.isDirectory()) {
      return Collections.emptyIterator();
    }

    String fileName = entry.getName();
    log.trace("Processing zip entry. Filename: {}", fileName);

    try {
      InputStream inputStream = zip.getInputStream(entry);
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

      return new Iterator<>() {
        private String nextLine = findNextValidLine();

        private String findNextValidLine() {
          try {
            String line;
            while ((line = reader.readLine()) != null) {
              String[] data = line.split(";");
              if (data[1].compareTo(startDate) >= 0 && data[1].compareTo(endDate) <= 0) {
                return line;
              }
            }
            return null;
          } catch (IOException e) {
            log.error("Error occurred while reading zip data: {}", e.getMessage());
            return null;
          }
        }

        @Override
        public boolean hasNext() {
          if (nextLine == null) {
            try {
              reader.close();
              inputStream.close();
            } catch (IOException e) {
              log.error("Error closing resources: {}", e.getMessage());
            }
            return false;
          }
          return true;
        }

        @Override
        public BarData next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          String currentLine = nextLine;
          nextLine = findNextValidLine();
          return parseBarDataFromLine(currentLine.split(";"));
        }
      };
    } catch (IOException e) {
      log.error("Error occurred while initializing zip data stream: {}", e.getMessage());
      return Collections.emptyIterator();
    }
  }

  private BarData parseBarDataFromLine(String[] data) {
    return new BarData(
      data[1],
      Double.parseDouble(data[2]),
      Double.parseDouble(data[4]),
      Double.parseDouble(data[5]),
      Double.parseDouble(data[3]),
      Long.parseLong(data[6])
    );
  }
}
