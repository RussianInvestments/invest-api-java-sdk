package ru.ttech.piapi.example.cmd;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import picocli.CommandLine;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.example.cmd.client.ClientCommand;
import ru.ttech.piapi.example.cmd.strategy.BacktestStrategyCommand;
import ru.ttech.piapi.example.cmd.strategy.LiveStrategyCommand;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(
  mixinStandardHelpOptions = true,
  description = "CLI для работы с API Т-Инвестиций",
  subcommands = {
    ClientCommand.class,
    BacktestStrategyCommand.class,
    LiveStrategyCommand.class
  }
)
public class MainCommand implements Runnable, ResponseFileWriter {

  @CommandLine.Option(
    names = {"-p", "--properties"},
    defaultValue = "invest.properties",
    showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
    description = "Имя файла с конфигурацией клиента"
  )
  private String propertiesFile;

  @CommandLine.Option(
    names = {"-o", "--output"},
    defaultValue = "output.json",
    showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
    description = "Имя файла для записи результата"
  )
  private String outputFile;

  public ConnectorConfiguration getConfiguration() {
    return ConnectorConfiguration.loadPropertiesFromFile(propertiesFile);
  }

  public ServiceStubFactory getFactory() {
    return ServiceStubFactory.create(getConfiguration());
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
    System.out.println("Необходима подкоманда: 'client', 'backtest' или 'live-trading' " +
      "Запустите программу с флагом -h или --help для получения подробной информации");
  }

  public static void main(String[] args) {
    new CommandLine(new MainCommand()).execute(args);
  }
}
