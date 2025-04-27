package ru.ttech.piapi.example.core;

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
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
import ru.ttech.piapi.core.impl.marketdata.subscription.CandleSubscriptionSpec;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;

import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MarketDataManagerExample {

  private static final Logger logger = LoggerFactory.getLogger(MarketDataManagerExample.class);

  public static void main(String[] args) {
    var configuration = ConnectorConfiguration.loadPropertiesFromResources("invest.properties");
    var unaryServiceFactory = ServiceStubFactory.create(configuration);
    var instrumentsService = unaryServiceFactory.newSyncService(InstrumentsServiceGrpc::newBlockingStub);
    // Получаем список всех акций
    var response = instrumentsService.callSyncMethod(stub -> stub.shares(InstrumentsRequest.getDefaultInstance()));
    // Фильтруем по доступности
    var availableInstruments = response.getInstrumentsList().stream()
      .filter(share -> share.getTradingStatus() == SecurityTradingStatus.SECURITY_TRADING_STATUS_DEALER_NORMAL_TRADING
        && share.getApiTradeAvailableFlag())
      .map(share -> new Instrument(share.getUid(), SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE))
      .collect(Collectors.toSet());
    logger.info("Total available shares: {}", availableInstruments.size());
    availableInstruments.forEach(instrument ->
      logger.debug("InstrumentUID: {}", instrument.getInstrumentUid())
    );
    var streamServiceFactory = StreamServiceStubFactory.create(unaryServiceFactory);
    var streamManagerFactory = StreamManagerFactory.create(streamServiceFactory);
    var executorService = Executors.newCachedThreadPool();
    var scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    var marketDataStreamManager = streamManagerFactory.newMarketDataStreamManager(executorService, scheduledExecutorService);
    // Подписываемся на свечи по инструментам
    OnNextListener<CandleWrapper> listener = candle -> logger.info("New candle incoming for instrument: {}", candle.getInstrumentUid());
    var subscriptionSpec = new CandleSubscriptionSpec();
    logger.info("Подписываемся");
    marketDataStreamManager.subscribeCandles(availableInstruments, subscriptionSpec, listener);
    marketDataStreamManager.start();
    try {
      Thread.sleep(10_000);
      // отписываемся
      logger.info("Отписываемся");
      marketDataStreamManager.unsubscribeCandles(availableInstruments, subscriptionSpec);
      Thread.sleep(10_000);
      // подписываемся заново
      logger.info("Подписываемся");
      marketDataStreamManager.subscribeCandles(availableInstruments, subscriptionSpec, listener);
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      marketDataStreamManager.shutdown();
    }
  }
}
