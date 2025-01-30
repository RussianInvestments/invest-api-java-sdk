package ru.ttech.piapi.springboot.storage.csv.repository;

import io.vavr.collection.Stream;
import org.apache.commons.csv.CSVFormat;
import ru.ttech.piapi.springboot.storage.core.repository.WriteRepository;
import ru.ttech.piapi.springboot.storage.csv.config.CsvConfiguration;
import ru.ttech.piapi.springboot.storage.csv.driver.CsvFileWriter;
import ru.ttech.piapi.springboot.storage.csv.driver.CsvWriter;

import java.io.IOException;

public abstract class CsvRepository<T> implements AutoCloseable, WriteRepository<T> {

  protected final CsvWriter csvWriter;

  public CsvRepository(CsvConfiguration configuration) throws IOException {
    var csvFormat = CSVFormat.Builder.create(CSVFormat.RFC4180)
      .setHeader(getHeaders())
      .setSkipHeaderRecord(true)
      .get();
    this.csvWriter = new CsvFileWriter(configuration.getOutputFile(), csvFormat);
  }

  @Override
  public Iterable<T> saveBatch(Iterable<T> entities) {
    csvWriter.writeBatch(Stream.ofAll(entities).map(this::convertToIterable));
    return entities;
  }

  @Override
  public T save(T entity) {
    csvWriter.write(convertToIterable(entity));
    return entity;
  }

  protected abstract String[] getHeaders();

  protected abstract Iterable<Object> convertToIterable(T entity);

  @Override
  public void close() throws IOException {
    csvWriter.close();
  }
}
