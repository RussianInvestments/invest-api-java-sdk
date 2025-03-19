package ru.ttech.piapi.example.cmd.client;


import picocli.CommandLine;
import ru.ttech.piapi.example.cmd.MainCommand;
import ru.ttech.piapi.example.cmd.client.instruments.InstrumentsCommand;

@CommandLine.Command(
  name = "client",
  mixinStandardHelpOptions = true,
  description = "Выполнение запросов к API",
  subcommands = {
    InstrumentsCommand.class
  }
)
public class ClientCommand implements Runnable {

  @CommandLine.ParentCommand
  private MainCommand parent;

  public MainCommand getParent() {
    return parent;
  }

  @Override
  public void run() {
    System.out.println("Необходима подкоманда: 'instruments'");
  }
}
