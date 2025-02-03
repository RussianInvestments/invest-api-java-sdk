package ru.ttech.piapi.storage.csv.driver;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CsvFileRecordIterator implements Iterator<CSVRecord> {

  private final Path inputFile;
  private final CSVFormat csvFormat;
  private CSVParser parser;
  private boolean initialized = false;
  protected Iterator<CSVRecord> iterator;

  public CsvFileRecordIterator(Path inputFile, CSVFormat csvFormat) {
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
