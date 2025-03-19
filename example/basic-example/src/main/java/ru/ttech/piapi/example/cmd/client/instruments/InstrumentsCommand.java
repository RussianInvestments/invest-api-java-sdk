package ru.ttech.piapi.example.cmd.client.instruments;

import picocli.CommandLine;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;
import ru.ttech.piapi.core.connector.resilience.ResilienceConfiguration;
import ru.ttech.piapi.core.connector.resilience.ResilienceSyncStubWrapper;
import ru.ttech.piapi.example.cmd.client.ClientCommand;

import java.util.concurrent.Executors;

@CommandLine.Command(
  name = "instruments",
  mixinStandardHelpOptions = true,
  description = "Получение списка различных типов инструментов",
  subcommands = {
    EtfsCommand.class, FuturesCommand.class, BondsCommand.class, SharesCommand.class, CurrenciesCommand.class, OptionsCommand.class
  }
)
public class InstrumentsCommand implements Runnable {

  public ClientCommand getParent() {
    return parent;
  }

  @CommandLine.ParentCommand
  private ClientCommand parent;

  public ResilienceSyncStubWrapper<InstrumentsServiceGrpc.InstrumentsServiceBlockingStub> getInstrumentsService() {
    var factory = parent.getParent().getFactory();
    var executorService = Executors.newSingleThreadScheduledExecutor();
    return factory.newResilienceSyncService(
      InstrumentsServiceGrpc::newBlockingStub,
      ResilienceConfiguration.builder(executorService, factory.getConfiguration())
        .build()
    );
  }

  @Override
  public void run() {
    System.out.println("Необходима подкоманда: 'shares', 'bonds', 'currencies', 'etfs', 'options' или 'futures'");
  }
}
