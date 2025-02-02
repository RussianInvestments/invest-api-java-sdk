package ru.ttech.piapi.springboot.storage.csv.config;

import java.nio.file.Path;

public class CsvConfiguration {

  private final Path outputFile;

  public CsvConfiguration(Path outputFile) {
    this.outputFile = outputFile;
  }

  public Path getOutputFile() {
    return outputFile;
  }
}
