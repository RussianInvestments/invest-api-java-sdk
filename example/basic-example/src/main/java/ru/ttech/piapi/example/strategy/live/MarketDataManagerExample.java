package ru.ttech.piapi.example.strategy.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentsRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;
import ru.tinkoff.piapi.contract.v1.SecurityTradingStatus;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.StreamManagerFactory;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;
import ru.ttech.piapi.example.strategy.backtest.BacktestExample;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.stream.Collectors;

public class MarketDataManagerExample {

  private static final Logger logger = LoggerFactory.getLogger(MarketDataManagerExample.class);

  public static void main(String[] args) {
    var properties = loadPropertiesFromFile("invest.properties");
    var configuration = ConnectorConfiguration.loadFromProperties(properties);
    var unaryServiceFactory = ServiceStubFactory.create(configuration);
    var instrumentsService = unaryServiceFactory.newSyncService(InstrumentsServiceGrpc::newBlockingStub);
    // Получаем список всех акций
    var response = instrumentsService.callSyncMethod(stub -> stub.shares(InstrumentsRequest.getDefaultInstance()));
    // Фильтруем по доступности
    var availableInstruments = response.getInstrumentsList().stream()
      .filter(share -> share.getTradingStatus() == SecurityTradingStatus.SECURITY_TRADING_STATUS_DEALER_NORMAL_TRADING
      && share.getApiTradeAvailableFlag())
      .map(share -> new Instrument(share.getUid(), SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE))
      .collect(Collectors.toList());
    logger.info("Total available shares: {}", availableInstruments.size());
    availableInstruments.forEach(instrument ->
      logger.debug("InstrumentUID: {}", instrument.getInstrumentUid())
    );
    var streamServiceFactory = StreamServiceStubFactory.create(unaryServiceFactory);
    var streamManagerFactory = StreamManagerFactory.create(streamServiceFactory);
    var marketDataStreamManager = streamManagerFactory.newMarketDataStreamManager();
    // Подписываемся на свечи по инструментам
    marketDataStreamManager.subscribeCandles(
      availableInstruments,
      GetCandlesRequest.CandleSource.CANDLE_SOURCE_INCLUDE_WEEKEND,
      candle -> logger.info("New candle incoming for instrument: {}", candle.getInstrumentUid())
      );
    try {
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      marketDataStreamManager.shutdown();
    }
  }

  private static Properties loadPropertiesFromFile(String filename) {
    Properties prop = new Properties();
    try (InputStream input = BacktestExample.class.getClassLoader().getResourceAsStream(filename)) {
      if (input == null) {
        throw new IllegalArgumentException("Невозможно загрузить файл настроек!");
      }
      prop.load(input);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return prop;
  }
}
