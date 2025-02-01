package ru.ttech.piapi.springboot.storage.csv.driver;

import java.io.IOException;

public interface CsvWriter extends AutoCloseable {

  void writeBatch(Iterable<Iterable<?>> rows);

  void write(Iterable<?> row);

  void close() throws IOException;
}
