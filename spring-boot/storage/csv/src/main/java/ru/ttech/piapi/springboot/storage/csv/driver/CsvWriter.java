package ru.ttech.piapi.springboot.storage.csv.driver;

import java.io.IOException;

public interface CsvWriter extends AutoCloseable {

  void writeBatch(Iterable<Iterable<Object>> rows);

  void write(Iterable<Object> row);

  void close() throws IOException;
}
