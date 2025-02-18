package ru.ttech.piapi.example.strategy.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.StreamManagerFactory;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.example.strategy.backtest.BacktestExample;
import ru.ttech.piapi.strategy.StrategyFactory;
import ru.ttech.piapi.strategy.candle.live.CandleStrategyConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class LiveCandleStrategyExample {

  private static final Logger logger = LoggerFactory.getLogger(LiveCandleStrategyExample.class);

  public static void main(String[] args) {
    var properties = loadPropertiesFromFile("invest.properties");
    var configuration = ConnectorConfiguration.loadFromProperties(properties);
    var factory = ServiceStubFactory.create(configuration);
    var streamFactory = StreamServiceStubFactory.create(factory);
    var streamManagerFactory = StreamManagerFactory.create(streamFactory);
    var executorService = Executors.newCachedThreadPool();
    var marketDataStreamManager = streamManagerFactory.newMarketDataStreamManager(executorService);
    var liveStrategyFactory = StrategyFactory.create(marketDataStreamManager);

    var ttechShare = CandleInstrument.newBuilder()
      .setInstrumentId("87db07bc-0e02-4e29-90bb-05e8ef791d7b")
      .setInterval(SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE)
      .build();
    var sberShare = CandleInstrument.newBuilder()
      .setInstrumentId("e6123145-9665-43e0-8413-cd61b8aa9b13")
      .setInterval(SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE)
      .build();
    var sberShareTwo = CandleInstrument.newBuilder()
      .setInstrumentId("e6123145-9665-43e0-8413-cd61b8aa9b13")
      .setInterval(SubscriptionInterval.SUBSCRIPTION_INTERVAL_2_MIN)
      .build();

    var strategy = liveStrategyFactory.newCandleStrategy(
      CandleStrategyConfiguration.builder()
        .setCandleSource(GetCandlesRequest.CandleSource.CANDLE_SOURCE_INCLUDE_WEEKEND)
        .setWarmupLength(100)
        .setStrategies(Map.of(
          ttechShare, createStrategy(5, 15),
          sberShare, createStrategy(10, 20),
          sberShareTwo, createStrategy(15, 25)
        ))
        .setStrategyEnterAction((candleInstrument, bar) ->
          logger.info("Entering strategy for instrument {} with interval {}",
            candleInstrument.getInstrumentId(),
            candleInstrument.getInterval()
          ))
        .setStrategyExitAction((candleInstrument, bar) ->
          logger.info("Exiting  strategy for instrument {} with interval {}",
            candleInstrument.getInstrumentId(),
            candleInstrument.getInterval()
          ))
        .build()
    );
    strategy.run();
    try {
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      marketDataStreamManager.shutdown();
    }
  }

  private static Function<BarSeries, Strategy> createStrategy(int shortEma, int longEma) {
    return barSeries -> {
      ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
      SMAIndicator shortSma = new SMAIndicator(closePrice, shortEma);
      SMAIndicator longSma = new SMAIndicator(closePrice, longEma);
      Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma);
      Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma);
      return new BaseStrategy(buyingRule, sellingRule);
    };
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
