package ru.ttech.piapi.example.cmd.marketdata;

import com.google.protobuf.MessageOrBuilder;
import picocli.CommandLine;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;
import ru.ttech.piapi.core.connector.resilience.ResilienceConfiguration;
import ru.ttech.piapi.core.connector.resilience.ResilienceSyncStubWrapper;
import ru.ttech.piapi.example.cmd.MainCommand;
import ru.ttech.piapi.example.cmd.ResponseFileWriter;
import ru.ttech.piapi.example.cmd.marketdata.instruments.SharesCommand;

import java.util.concurrent.Executors;

@CommandLine.Command(
  name = "instruments",
  mixinStandardHelpOptions = true,
  description = "Получение списка различных типов инструментов",
  subcommands = {
    SharesCommand.class
  }
)
public class InstrumentsCommand implements Runnable, ResponseFileWriter {

  @CommandLine.ParentCommand
  private MainCommand parent;

  public MainCommand getMainCommand() {
    return parent;
  }

  public ResilienceSyncStubWrapper<InstrumentsServiceGrpc.InstrumentsServiceBlockingStub> getInstrumentsService() {
    var factory = parent.getFactory();
    var executorService = Executors.newSingleThreadScheduledExecutor();
    return factory.newResilienceSyncService(
      InstrumentsServiceGrpc::newBlockingStub,
      ResilienceConfiguration.builder(executorService, factory.getConfiguration())
        .build()
    );
  }

  @Override
  public void run() {
    System.out.println("Необходима подкоманда: 'shares', 'bonds'");
  }

  @Override
  public void writeResponseToFile(MessageOrBuilder response) {
    parent.writeResponseToFile(response);
  }
}
