package ru.ttech.piapi.example.cmd.client.instruments;

import picocli.CommandLine;
import ru.tinkoff.piapi.contract.v1.InstrumentsRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;

@CommandLine.Command(
  name = "bonds",
  mixinStandardHelpOptions = true,
  description = "Получение списка облигаций"
)
public class BondsCommand implements Runnable {

  @CommandLine.ParentCommand
  private InstrumentsCommand instrumentsCommand;

  @Override
  public void run() {
    var instrumentsService = instrumentsCommand.getInstrumentsService();
    var response = instrumentsService.callSyncMethod(
      InstrumentsServiceGrpc.getBondsMethod(),
      stub -> stub.bonds(InstrumentsRequest.getDefaultInstance())
    );
    instrumentsCommand.getParent().getParent().writeResponseToFile(response);
  }
}
