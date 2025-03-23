package ru.ttech.piapi.example.cmd.client.instruments;

import picocli.CommandLine;
import ru.tinkoff.piapi.contract.v1.CurrenciesResponse;
import ru.tinkoff.piapi.contract.v1.InstrumentsRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;

import java.util.stream.Collectors;

@CommandLine.Command(
  name = "currencies",
  mixinStandardHelpOptions = true,
  description = "Получение списка валют"
)
public class CurrenciesCommand implements Runnable {

  @CommandLine.ParentCommand
  private InstrumentsCommand instrumentsCommand;

  @Override
  public void run() {
    var instrumentsService = instrumentsCommand.getInstrumentsService();
    var response = instrumentsService.callSyncMethod(
      InstrumentsServiceGrpc.getCurrenciesMethod(),
      stub -> stub.currencies(InstrumentsRequest.getDefaultInstance())
    );
    if (!instrumentsCommand.getTicker().isBlank()) {
      var filteredInstruments = response.getInstrumentsList().stream()
        .filter(currency -> currency.getTicker().equals(instrumentsCommand.getTicker()))
        .collect(Collectors.toList());
      var filteredResponse = CurrenciesResponse.newBuilder()
        .addAllInstruments(filteredInstruments)
        .build();
      instrumentsCommand.getParent().getParent().writeResponseToFile(filteredResponse);
    } else {
      instrumentsCommand.getParent().getParent().writeResponseToFile(response);
    }
  }
}
