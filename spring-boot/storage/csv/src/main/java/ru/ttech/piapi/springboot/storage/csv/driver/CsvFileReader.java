package ru.ttech.piapi.springboot.storage.csv.driver;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class CsvFileReader implements CsvReader {

  private final CSVFormat csvFormat;
  private final Path inputFile;

  public CsvFileReader(Path inputFile, CSVFormat csvFormat) {
    this.csvFormat = csvFormat;
    this.inputFile = inputFile;
  }

  @Override
  public Iterable<CSVRecord> findAll() {
    return () -> new CsvRecordIterator(inputFile, csvFormat);
  }

  @Override
  public Iterable<CSVRecord> findAllByPrefix(String prefix) {
    return () -> new CsvRecordIterator(inputFile, csvFormat) {
      private CSVRecord nextRecord;

      @Override
      public boolean hasNext() {
        initialize();
        while (iterator.hasNext()) {
          nextRecord = iterator.next();
          var line = nextRecord.stream().collect(Collectors.joining(","));
          if (line.startsWith(prefix)) {
            return true;
          }
        }
        nextRecord = null;
        close();
        return false;
      }

      @Override
      public CSVRecord next() {
        if (nextRecord == null) {
          throw new NoSuchElementException("No more records matching the prefix");
        }
        return nextRecord;
      }
    };
  }

  protected static class CsvRecordIterator implements Iterator<CSVRecord> {

    private final Path inputFile;
    private final CSVFormat csvFormat;
    private CSVParser parser;
    private boolean initialized = false;
    protected Iterator<CSVRecord> iterator;

    public CsvRecordIterator(Path inputFile, CSVFormat csvFormat) {
      this.inputFile = inputFile;
      this.csvFormat = csvFormat;
    }

    @Override
    public boolean hasNext() {
      initialize();
      if (iterator.hasNext()) {
        return true;
      }
      close();
      return false;
    }

    @Override
    public CSVRecord next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return iterator.next();
    }

    protected void initialize() {
      if (!initialized) {
        try {
          parser = CSVParser.parse(this.inputFile, StandardCharsets.UTF_8, this.csvFormat);
          iterator = parser.iterator();
          initialized = true;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }

    protected void close() {
      if (parser != null) {
        try {
          parser.close();
          initialized = false;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
}
