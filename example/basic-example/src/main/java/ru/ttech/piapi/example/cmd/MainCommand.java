package ru.ttech.piapi.example.cmd;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import picocli.CommandLine;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.example.cmd.marketdata.InstrumentsCommand;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(
  name = "piapi",
  mixinStandardHelpOptions = true,
  description = "CLI для работы с API Т-Инвестиций",
  subcommands = {
    InstrumentsCommand.class
  })
public class MainCommand implements Runnable, ResponseFileWriter {

  @CommandLine.Parameters(index = "0", description = "Имя файла с конфигурацией клиента")
  private String propertiesFile;

  @CommandLine.Parameters(index = "1", description = "Имя файла для записи результата")
  private String outputFile;

  public ServiceStubFactory getFactory() {
    var configuration = ConnectorConfiguration.loadProperties(propertiesFile);
    return ServiceStubFactory.create(configuration);
  }

  @Override
  public void writeResponseToFile(MessageOrBuilder response) {
    try (BufferedWriter writer = Files.newBufferedWriter(Path.of(outputFile))) {
      writer.write(JsonFormat.printer().print(response));
      System.out.println("Результат записан в файл: " + outputFile);
    } catch (IOException e) {
      System.err.println("Произошла ошибка при записи в файл: " + e.getMessage());
    }
  }

  @Override
  public void run() {
    System.out.println("Необходима подкоманда: 'instruments'");
  }

  public static void main(String[] args) {
    new CommandLine(new MainCommand()).execute(args);
  }
}
