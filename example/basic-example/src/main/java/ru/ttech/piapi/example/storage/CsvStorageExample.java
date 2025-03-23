package ru.ttech.piapi.example.storage;

import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.StreamManagerFactory;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.impl.marketdata.subscription.CandleSubscriptionSpec;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;
import ru.ttech.piapi.storage.csv.config.CsvConfiguration;
import ru.ttech.piapi.storage.csv.repository.CandlesCsvRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Executors;

public class CsvStorageExample {

  public static void main(String[] args) {
    var connectorConfiguration = ConnectorConfiguration.loadPropertiesFromResources("invest.properties");
    var unaryServiceFactory = ServiceStubFactory.create(connectorConfiguration);
    var streamServiceFactory = StreamServiceStubFactory.create(unaryServiceFactory);
    var streamManagerFactory = StreamManagerFactory.create(streamServiceFactory);
    var executorService = Executors.newCachedThreadPool();
    var scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    CsvConfiguration csvConfiguration = new CsvConfiguration(Path.of("candles.csv"));
    try (var candlesRepository = new CandlesCsvRepository(csvConfiguration)) {
      var marketDataStreamManager = streamManagerFactory.newMarketDataStreamManager(executorService, scheduledExecutorService);
      marketDataStreamManager.subscribeCandles(Set.of(
          new Instrument(
            "87db07bc-0e02-4e29-90bb-05e8ef791d7b",
            SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE
          )
        ),
        new CandleSubscriptionSpec(),
        candle -> candlesRepository.save(candle.getOriginal())
      );
      marketDataStreamManager.start();

      try {
        Thread.currentThread().join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
