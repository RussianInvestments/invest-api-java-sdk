package ru.ttech.piapi.example.storage;

import org.postgresql.ds.PGSimpleDataSource;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.StreamManagerFactory;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.impl.marketdata.subscription.CandleSubscriptionSpec;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;
import ru.ttech.piapi.storage.jdbc.config.JdbcConfiguration;
import ru.ttech.piapi.storage.jdbc.repository.CandlesJdbcRepository;

import javax.sql.DataSource;
import java.util.Set;
import java.util.concurrent.Executors;

@SuppressWarnings("DuplicatedCode")
public class JdbcStorageExample {

  public static void main(String[] args) {
    var connectorConfiguration = ConnectorConfiguration.loadFromPropertiesFile("invest.properties");
    var unaryServiceFactory = ServiceStubFactory.create(connectorConfiguration);
    var streamServiceFactory = StreamServiceStubFactory.create(unaryServiceFactory);
    var streamManagerFactory = StreamManagerFactory.create(streamServiceFactory);
    var executorService = Executors.newCachedThreadPool();
    var scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    var jdbcConfiguration = new JdbcConfiguration(createDataSource(), "trading", "candles");
    var candlesRepository = new CandlesJdbcRepository(jdbcConfiguration);
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
  }

  private static DataSource createDataSource() {
    var pgDataSource = new PGSimpleDataSource();
    pgDataSource.setUrl("jdbc:postgresql://localhost:5432/invest");
    pgDataSource.setUser("user");
    pgDataSource.setPassword("password");
    return pgDataSource;
  }
}
