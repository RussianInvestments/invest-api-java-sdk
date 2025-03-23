package ru.ttech.piapi.example.strategy.backtest;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.InstrumentsRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;
import ru.tinkoff.piapi.contract.v1.SecurityTradingStatus;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.strategy.candle.backtest.BarsLoader;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class BarsLoadingExample {

  public static void main(String[] args) {
    var configuration = ConnectorConfiguration.loadPropertiesFromResources("invest.properties");
    var unaryServiceFactory = ServiceStubFactory.create(configuration);
    var barsLoader = new BarsLoadingExample();
    var from = LocalDate.of(2018, 1, 1);
    var interval = CandleInterval.CANDLE_INTERVAL_1_MIN;
    var instrumentsService = unaryServiceFactory.newSyncService(InstrumentsServiceGrpc::newBlockingStub);
    var response = instrumentsService.callSyncMethod(stub -> stub.shares(InstrumentsRequest.getDefaultInstance()));
    var instruments = response.getInstrumentsList().stream()
      .filter(share -> share.getTradingStatus() == SecurityTradingStatus.SECURITY_TRADING_STATUS_DEALER_NORMAL_TRADING
        && share.getApiTradeAvailableFlag())
      .map(share -> Tuple.of(share.getUid(), interval))
      .collect(Collectors.toList());
    barsLoader.loadBars(configuration, instruments, from);
  }

  public void loadBars(ConnectorConfiguration configuration, List<Tuple2<String, CandleInterval>> instruments, LocalDate from) {
    var executorService = Executors.newCachedThreadPool();
    var barsLoader = new BarsLoader(null, configuration, executorService);
    instruments.forEach(instrument -> {
      CandleInterval interval = instrument._2();
      String filename = String.format("%s_%s.csv", instrument._1(), interval.name().toLowerCase()).toLowerCase();
      var bars = barsLoader.loadBars(instrument._1(), interval, from);
      barsLoader.saveBars(Path.of(filename), bars);
    });
    executorService.shutdown();
  }
}
