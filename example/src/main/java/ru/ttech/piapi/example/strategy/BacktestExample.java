package ru.ttech.piapi.example.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.AnalysisCriterion;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.criteria.pnl.ProfitCriterion;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.strategy.StrategyFactory;
import ru.ttech.piapi.strategy.candle.backtest.CandleStrategyBacktestConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Properties;
import java.util.function.Function;

@SuppressWarnings("DuplicatedCode")
public class BacktestExample {

  private static final Logger logger = LoggerFactory.getLogger(BacktestExample.class);

  public static void main(String[] args) {
    var properties = loadPropertiesFromFile("invest.properties");
    var configuration = ConnectorConfiguration.loadFromProperties(properties);
    var factory = ServiceStubFactory.create(configuration);
    var streamFactory = StreamServiceStubFactory.create(factory);
    var strategyFactory = StrategyFactory.create(streamFactory);

    Function<BarSeries, Strategy> tradingStrategy = barSeries -> {
      ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
      SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
      SMAIndicator longSma = new SMAIndicator(closePrice, 30);
      Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma);
      Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma);
      return new BaseStrategy(buyingRule, sellingRule);
    };

    var backtest = strategyFactory.newCandleStrategyBacktest(
      CandleStrategyBacktestConfiguration.builder()
        .setInstrument(CandleInstrument.newBuilder()
          .setInstrumentId("87db07bc-0e02-4e29-90bb-05e8ef791d7b")
          .setInterval(SubscriptionInterval.SUBSCRIPTION_INTERVAL_30_MIN)
          .build())
        .setFrom(LocalDate.of(2025, 1, 1))
        .setTo(LocalDate.of(2025, 2, 5))
        .setStrategy(tradingStrategy)
        .setStrategyAnalysis(((barSeries, tradingRecord) -> {
          AnalysisCriterion criterion = new ProfitCriterion();
          var profit = criterion.calculate(barSeries, tradingRecord);
          logger.info("Profit: {}", profit);
        }))
        .build());
    backtest.run();
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
