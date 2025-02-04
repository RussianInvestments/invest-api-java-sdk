package ru.ttech.piapi.storage.csv.driver;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.nio.file.Path;
import java.util.NoSuchElementException;

public class CsvFileReader implements CsvReader {

  private final CSVFormat csvFormat;
  private final Path inputFile;

  public CsvFileReader(Path inputFile, CSVFormat csvFormat) {
    this.csvFormat = csvFormat;
    this.inputFile = inputFile;
  }

  @Override
  public Iterable<CSVRecord> findAll() {
    return () -> new CsvFileRecordIterator(inputFile, csvFormat);
  }

  @Override
  public Iterable<CSVRecord> findByTimeAndInstrumentUid(String time, String instrumentUid) {
    return () -> new CsvFileRecordFindIterator(inputFile, csvFormat) {

      @Override
      public boolean hasNext() {
        initialize();
        while (iterator.hasNext()) {
          nextRecord = iterator.next();
          if (nextRecord.get(0).equals(time) && nextRecord.get(1).equals(instrumentUid)) {
            return true;
          }
        }
        nextRecord = null;
        close();
        return false;
      }
    };
  }

  @Override
  public Iterable<CSVRecord> findByPeriodAndInstrumentUid(String startTime, String endTime, String instrumentUid) {
    return () -> new CsvFileRecordFindIterator(inputFile, csvFormat) {

      @Override
      public boolean hasNext() {
        initialize();
        while (iterator.hasNext()) {
          nextRecord = iterator.next();
          if (nextRecord.get(0).compareTo(startTime) >= 0
            && nextRecord.get(0).compareTo(endTime) <= 0
            && nextRecord.get(1).equals(instrumentUid)
          ) {
            return true;
          }
        }
        nextRecord = null;
        close();
        return false;
      }
    };
  }

  private static class CsvFileRecordFindIterator extends CsvFileRecordIterator {

    protected CSVRecord nextRecord;

    public CsvFileRecordFindIterator(Path inputFile, CSVFormat csvFormat) {
      super(inputFile, csvFormat);
    }

    @Override
    public CSVRecord next() {
      if (nextRecord == null) {
        throw new NoSuchElementException("No more records matching the prefix");
      }
      return nextRecord;
    }
  }
}
