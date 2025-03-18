package ru.ttech.piapi.example.cmd.marketdata.instruments;

import com.google.protobuf.MessageOrBuilder;
import picocli.CommandLine;
import ru.tinkoff.piapi.contract.v1.InstrumentsRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;
import ru.ttech.piapi.example.cmd.ResponseFileWriter;
import ru.ttech.piapi.example.cmd.marketdata.InstrumentsCommand;

@CommandLine.Command(
  name = "shares",
  mixinStandardHelpOptions = true,
  description = "Получение списка акций"
)
public class SharesCommand implements Runnable, ResponseFileWriter {

  @CommandLine.ParentCommand
  private InstrumentsCommand parent;

  @Override
  public void run() {
    var instrumentsService = parent.getInstrumentsService();
    var response = instrumentsService.callSyncMethod(
      InstrumentsServiceGrpc.getSharesMethod(),
      stub -> stub.shares(InstrumentsRequest.getDefaultInstance())
    );
    writeResponseToFile(response);
  }

  @Override
  public void writeResponseToFile(MessageOrBuilder response) {
    parent.writeResponseToFile(response);
  }
}
