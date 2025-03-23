package ru.ttech.piapi.example.cmd.client.instruments;

import picocli.CommandLine;
import ru.tinkoff.piapi.contract.v1.EtfsResponse;
import ru.tinkoff.piapi.contract.v1.InstrumentsRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;

import java.util.stream.Collectors;

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
    if (!instrumentsCommand.getTicker().isBlank()) {
      var filteredInstruments = response.getInstrumentsList().stream()
        .filter(etf -> etf.getTicker().equals(instrumentsCommand.getTicker()))
        .collect(Collectors.toList());
      var filteredResponse = EtfsResponse.newBuilder()
        .addAllInstruments(filteredInstruments)
        .build();
      instrumentsCommand.getParent().getParent().writeResponseToFile(filteredResponse);
    } else {
      instrumentsCommand.getParent().getParent().writeResponseToFile(response);
    }
  }
}
