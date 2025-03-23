package ru.ttech.piapi.example.cmd.client.instruments;

import picocli.CommandLine;
import ru.tinkoff.piapi.contract.v1.FuturesResponse;
import ru.tinkoff.piapi.contract.v1.InstrumentsRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;

import java.util.stream.Collectors;

@CommandLine.Command(
  name = "futures",
  mixinStandardHelpOptions = true,
  description = "Получение списка фьючерсов"
)
public class FuturesCommand implements Runnable {

  @CommandLine.ParentCommand
  private InstrumentsCommand instrumentsCommand;

  @Override
  public void run() {
    var instrumentsService = instrumentsCommand.getInstrumentsService();
    var response = instrumentsService.callSyncMethod(
      InstrumentsServiceGrpc.getFuturesMethod(),
      stub -> stub.futures(InstrumentsRequest.getDefaultInstance())
    );
    if (!instrumentsCommand.getTicker().isBlank()) {
      var filteredInstruments = response.getInstrumentsList().stream()
        .filter(futures -> futures.getTicker().equals(instrumentsCommand.getTicker()))
        .collect(Collectors.toList());
      var filteredResponse = FuturesResponse.newBuilder()
        .addAllInstruments(filteredInstruments)
        .build();
      instrumentsCommand.getParent().getParent().writeResponseToFile(filteredResponse);
    } else {
      instrumentsCommand.getParent().getParent().writeResponseToFile(response);
    }
  }
}
