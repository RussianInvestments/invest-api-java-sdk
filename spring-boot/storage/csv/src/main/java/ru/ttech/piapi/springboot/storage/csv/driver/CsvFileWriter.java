package ru.ttech.piapi.springboot.storage.csv.driver;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class CsvFileWriter implements CsvWriter {

  private final CSVPrinter printer;
  private final Writer writer;

  public CsvFileWriter(String outputFile, CSVFormat csvFormat) throws IOException {
    Path path = Paths.get(outputFile);
    boolean needsHeader = !Files.exists(path);
    this.writer = Files.newBufferedWriter(
      path,
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
  public void writeBatch(Iterable<Iterable<Object>> rows) {
    try {
      for (Iterable<Object> row : rows) {
        printer.printRecord(row);
      }
      printer.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void write(Iterable<Object> row) {
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
