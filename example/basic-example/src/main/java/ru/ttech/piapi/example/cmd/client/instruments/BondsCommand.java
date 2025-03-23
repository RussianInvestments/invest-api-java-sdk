package ru.ttech.piapi.example.cmd.client.instruments;

import picocli.CommandLine;
import ru.tinkoff.piapi.contract.v1.BondsResponse;
import ru.tinkoff.piapi.contract.v1.InstrumentsRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;

import java.util.stream.Collectors;

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
    if (!instrumentsCommand.getTicker().isBlank()) {
      var filteredInstruments = response.getInstrumentsList().stream()
        .filter(bond -> bond.getTicker().equals(instrumentsCommand.getTicker()))
        .collect(Collectors.toList());
      var filteredResponse = BondsResponse.newBuilder()
        .addAllInstruments(filteredInstruments)
        .build();
      instrumentsCommand.getParent().getParent().writeResponseToFile(filteredResponse);
    } else {
      instrumentsCommand.getParent().getParent().writeResponseToFile(response);
    }
  }
}
