package ru.ttech.piapi.springboot.storage.csv.config;

public class CsvConfiguration {

  private final String outputFile;

  public CsvConfiguration(String outputFile) {
    this.outputFile = outputFile;
  }

  public String getOutputFile() {
    return outputFile;
  }
}
