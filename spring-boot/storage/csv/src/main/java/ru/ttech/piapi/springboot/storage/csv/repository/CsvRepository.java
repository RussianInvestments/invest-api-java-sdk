package ru.ttech.piapi.springboot.storage.csv.repository;

import io.vavr.collection.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import ru.ttech.piapi.springboot.storage.core.repository.ReadWriteRepository;
import ru.ttech.piapi.springboot.storage.csv.config.CsvConfiguration;
import ru.ttech.piapi.springboot.storage.csv.driver.CsvFileReader;
import ru.ttech.piapi.springboot.storage.csv.driver.CsvFileWriter;
import ru.ttech.piapi.springboot.storage.csv.driver.CsvReader;
import ru.ttech.piapi.springboot.storage.csv.driver.CsvWriter;

import java.io.IOException;
import java.time.LocalDateTime;

public abstract class CsvRepository<T> implements AutoCloseable, ReadWriteRepository<T> {

  protected final CsvWriter csvWriter;
  protected final CsvReader csvReader;

  public CsvRepository(CsvConfiguration configuration) throws IOException {
    var csvFormat = CSVFormat.Builder.create(CSVFormat.RFC4180)
      .setHeader(getHeaders())
      .setSkipHeaderRecord(true)
      .get();
    this.csvWriter = new CsvFileWriter(configuration.getOutputFile(), csvFormat);
    this.csvReader = new CsvFileReader(configuration.getOutputFile(), csvFormat);
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

  @Override
  public Iterable<T> findAll() {
    return Stream.ofAll(csvReader.findAll())
      .map(this::convertToEntity);
  }

  @Override
  public Iterable<T> findAllByTimeAndInstrumentUid(LocalDateTime time, String instrumentUid) {
    return Stream.ofAll(csvReader.findByTimeAndInstrumentUid(time.toString(), instrumentUid))
      .map(this::convertToEntity);
  }

  @Override
  public Iterable<T> findAllByPeriodAndInstrumentUid(
    LocalDateTime startTime,
    LocalDateTime endTime,
    String instrumentUid
  ) {
    return Stream.ofAll(csvReader.findByPeriodAndInstrumentUid(startTime.toString(), endTime.toString(), instrumentUid))
      .map(this::convertToEntity);
  }

  protected abstract String[] getHeaders();

  protected abstract Iterable<?> convertToIterable(T entity);

  protected abstract T convertToEntity(CSVRecord csvRecord);

  @Override
  public void close() throws IOException {
    csvWriter.close();
  }
}
