package ru.ttech.piapi.strategy;

import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.strategy.candle.CandleStrategyConfiguration;
import ru.ttech.piapi.strategy.candle.StrategyFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Example {

  public static void main(String[] args) {
    var properties = loadPropertiesFromFile("invest.properties");
    var configuration = ConnectorConfiguration.loadFromProperties(properties);
    var factory = ServiceStubFactory.create(configuration);
    var streamFactory = StreamServiceStubFactory.create(factory);
    var strategyFactory = StrategyFactory.create(streamFactory);

    var strategy = strategyFactory.newCandleStrategy(
      CandleStrategyConfiguration.builder()
        .setInstrument(CandleInstrument.newBuilder()
          .setInstrumentId("87db07bc-0e02-4e29-90bb-05e8ef791d7b")
          .setInterval(SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE)
          .build())
        .setCandleSource(GetCandlesRequest.CandleSource.CANDLE_SOURCE_INCLUDE_WEEKEND)
        .setWarmupLength(100)
        .setStrategy(barSeries -> {
          ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
          SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
          SMAIndicator longSma = new SMAIndicator(closePrice, 30);
          Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma);
          Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma);
          return new BaseStrategy(buyingRule, sellingRule);
        })
        .build());
    strategy.run();

    // томрозим основной поток
    try {
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static Properties loadPropertiesFromFile(String filename) {
    Properties prop = new Properties();
    try (InputStream input = Example.class.getClassLoader().getResourceAsStream(filename)) {
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
