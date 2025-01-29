package ru.ttech.piapi.springboot.storage.csv.driver;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class CsvWriter {

  public void write(String outputFile, CSVFormat csvFormat, String row) {
    try (Writer writer = Files.newBufferedWriter(Paths.get(outputFile),
      StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
         CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
      csvPrinter.printRecord(row);
      csvPrinter.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
