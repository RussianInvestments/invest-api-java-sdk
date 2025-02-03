package ru.ttech.piapi.springboot.storage.csv.driver;

import org.apache.commons.csv.CSVRecord;

public interface CsvReader {

  Iterable<CSVRecord> findAll();

  Iterable<CSVRecord> findByTimeAndInstrumentUid(String time, String instrumentUid);

  Iterable<CSVRecord> findByPeriodAndInstrumentUid(String startTime, String endTime, String instrumentUid);
}
