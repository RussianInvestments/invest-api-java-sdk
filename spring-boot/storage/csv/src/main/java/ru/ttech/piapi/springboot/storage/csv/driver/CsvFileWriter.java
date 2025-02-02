package ru.ttech.piapi.springboot.storage.csv.driver;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class CsvFileWriter implements CsvWriter {

  private final CSVPrinter printer;
  private final BufferedWriter writer;

  public CsvFileWriter(Path outputFile, CSVFormat csvFormat) throws IOException {
    boolean needsHeader = !Files.exists(outputFile);
    this.writer = Files.newBufferedWriter(
      outputFile,
      StandardCharsets.UTF_8,
      StandardOpenOption.CREATE,
      StandardOpenOption.APPEND
    );
    this.printer = new CSVPrinter(writer, csvFormat);

    if (needsHeader) {
      printer.printRecord((Object[]) csvFormat.getHeader());
      printer.flush();
    }
  }

  @Override
  public synchronized void writeBatch(Iterable<Iterable<?>> rows) {
    try {
      for (Iterable<?> row : rows) {
        printer.printRecord(row);
      }
      printer.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public synchronized void write(Iterable<?> row) {
    try {
      printer.printRecord(row);
      printer.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() throws IOException {
    printer.close();
    writer.close();
  }
}
