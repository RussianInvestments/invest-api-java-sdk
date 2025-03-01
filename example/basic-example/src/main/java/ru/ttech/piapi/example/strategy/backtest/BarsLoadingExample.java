package ru.ttech.piapi.example.strategy.backtest;

import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.InstrumentsRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;
import ru.tinkoff.piapi.contract.v1.SecurityTradingStatus;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.strategy.candle.backtest.BarsLoader;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.Executors;

public class BarsLoadingExample {

  public static void main(String[] args) {
    var configuration = ConnectorConfiguration.loadFromPropertiesFile("invest.properties");
    var unaryServiceFactory = ServiceStubFactory.create(configuration);
    var executorService = Executors.newCachedThreadPool();
    var barsLoader = new BarsLoader(configuration, executorService);
    var instrumentsService = unaryServiceFactory.newSyncService(InstrumentsServiceGrpc::newBlockingStub);
    // получаем список всех акций
    var response = instrumentsService.callSyncMethod(stub -> stub.shares(InstrumentsRequest.getDefaultInstance()));
    // фильтруем по доступности, загружаем архивы минутных свечей и формируем файл с агрегированными свечами
    response.getInstrumentsList().stream()
      .filter(share -> share.getTradingStatus() == SecurityTradingStatus.SECURITY_TRADING_STATUS_DEALER_NORMAL_TRADING
        && share.getApiTradeAvailableFlag())
      .forEach(share -> {
        LocalDate from = TimeMapper.timestampToLocalDateTime(share.getFirst1MinCandleDate()).toLocalDate();
        String instrumentId = share.getUid();
        CandleInterval interval = CandleInterval.CANDLE_INTERVAL_1_MIN; // заменить на нужный
        String filename = String.format("%s_%s.csv", instrumentId, interval).toLowerCase();
        var bars = barsLoader.loadBars(instrumentId, interval, from);
        barsLoader.saveBars(Path.of(filename), bars);
      });
    System.out.println("Stop");
    executorService.shutdown();
  }
}
