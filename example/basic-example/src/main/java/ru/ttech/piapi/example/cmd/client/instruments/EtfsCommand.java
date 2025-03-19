package ru.ttech.piapi.example.cmd.client.instruments;

import picocli.CommandLine;
import ru.tinkoff.piapi.contract.v1.InstrumentsRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;

@CommandLine.Command(
  name = "etfs",
  mixinStandardHelpOptions = true,
  description = "Получение списка инвестиционных фондов"
)
public class EtfsCommand implements Runnable {

  @CommandLine.ParentCommand
  private InstrumentsCommand instrumentsCommand;

  @Override
  public void run() {
    var instrumentsService = instrumentsCommand.getInstrumentsService();
    var response = instrumentsService.callSyncMethod(
      InstrumentsServiceGrpc.getEtfsMethod(),
      stub -> stub.etfs(InstrumentsRequest.getDefaultInstance())
    );
    instrumentsCommand.getParent().getParent().writeResponseToFile(response);
  }
}
