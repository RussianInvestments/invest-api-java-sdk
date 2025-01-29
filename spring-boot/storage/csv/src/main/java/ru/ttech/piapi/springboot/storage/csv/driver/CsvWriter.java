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

public class CsvWriter {

  public void write(String outputFile, CSVFormat csvFormat, Object... row) {
    Path path = Paths.get(outputFile);
    try {
      if (!Files.exists(path)) {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
             var printer = new CSVPrinter(writer, csvFormat)) {
          printer.printRecord((Object[]) csvFormat.getHeader());
        }
      }
      try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
           var printer = new CSVPrinter(writer, csvFormat)) {
        printer.printRecord(row);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
