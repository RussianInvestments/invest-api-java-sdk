package ru.ttech.piapi.example.strategy.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.InstrumentsRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;
import ru.tinkoff.piapi.contract.v1.SecurityTradingStatus;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.StreamManagerFactory;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.impl.marketdata.subscription.CandleSubscriptionSpec;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;

import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MarketDataManagerExample {

  private static final Logger logger = LoggerFactory.getLogger(MarketDataManagerExample.class);

  public static void main(String[] args) {
    var configuration = ConnectorConfiguration.loadFromPropertiesFile("invest.properties");
    var unaryServiceFactory = ServiceStubFactory.create(configuration);
    var instrumentsService = unaryServiceFactory.newSyncService(InstrumentsServiceGrpc::newBlockingStub);
    // Получаем список всех акций
    var response = instrumentsService.callSyncMethod(stub -> stub.shares(InstrumentsRequest.getDefaultInstance()));
    // Фильтруем по доступности
    var availableInstruments = response.getInstrumentsList().stream()
      .filter(share -> share.getTradingStatus() == SecurityTradingStatus.SECURITY_TRADING_STATUS_NORMAL_TRADING
        && share.getApiTradeAvailableFlag())
      .map(share -> new Instrument(share.getUid(), SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE))
      .collect(Collectors.toList());
    logger.info("Total available shares: {}", availableInstruments.size());
    availableInstruments.forEach(instrument ->
      logger.debug("InstrumentUID: {}", instrument.getInstrumentUid())
    );
    var streamServiceFactory = StreamServiceStubFactory.create(unaryServiceFactory);
    var streamManagerFactory = StreamManagerFactory.create(streamServiceFactory);
    var executorService = Executors.newCachedThreadPool();
    var marketDataStreamManager = streamManagerFactory.newMarketDataStreamManager(executorService);
    // Подписываемся на свечи по инструментам
    marketDataStreamManager.subscribeCandles(
      availableInstruments,
      new CandleSubscriptionSpec(),
      candle -> logger.info("New candle incoming for instrument: {}", candle.getInstrumentUid())
    );
    marketDataStreamManager.start();
    try {
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      marketDataStreamManager.shutdown();
    }
  }
}
