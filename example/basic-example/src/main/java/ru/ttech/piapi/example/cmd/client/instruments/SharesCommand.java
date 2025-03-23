package ru.ttech.piapi.example.cmd.client.instruments;

import picocli.CommandLine;
import ru.tinkoff.piapi.contract.v1.InstrumentsRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;
import ru.tinkoff.piapi.contract.v1.SharesResponse;

import java.util.stream.Collectors;

@CommandLine.Command(
  name = "shares",
  mixinStandardHelpOptions = true,
  description = "Получение списка акций"
)
public class SharesCommand implements Runnable {
  @CommandLine.ParentCommand
  private InstrumentsCommand instrumentsCommand;

  @Override
  public void run() {
    var instrumentsService = instrumentsCommand.getInstrumentsService();
    var response = instrumentsService.callSyncMethod(
      InstrumentsServiceGrpc.getSharesMethod(),
      stub -> stub.shares(InstrumentsRequest.getDefaultInstance())
    );
    if (!instrumentsCommand.getTicker().isBlank()) {
      var filteredInstruments = response.getInstrumentsList().stream()
        .filter(share -> share.getTicker().equals(instrumentsCommand.getTicker()))
        .collect(Collectors.toList());
      var filteredResponse = SharesResponse.newBuilder()
        .addAllInstruments(filteredInstruments)
        .build();
      instrumentsCommand.getParent().getParent().writeResponseToFile(filteredResponse);
    } else {
      instrumentsCommand.getParent().getParent().writeResponseToFile(response);
    }
  }
}
