package ru.ttech.piapi.springboot.storage.csv.driver;

import org.apache.commons.csv.CSVRecord;

public interface CsvReader {

  Iterable<CSVRecord> findAll();

  Iterable<CSVRecord> findAllByPrefix(String prefix);
}
