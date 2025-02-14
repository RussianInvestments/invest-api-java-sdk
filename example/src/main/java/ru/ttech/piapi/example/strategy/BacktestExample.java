package ru.ttech.piapi.example.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.AnalysisCriterion;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.analysis.cost.LinearTransactionCostModel;
import org.ta4j.core.backtest.TradeOnCurrentCloseModel;
import org.ta4j.core.criteria.pnl.ProfitCriterion;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.strategy.StrategyFactory;
import ru.ttech.piapi.strategy.candle.backtest.CandleStrategyBacktestConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.function.Function;

@SuppressWarnings("DuplicatedCode")
public class BacktestExample {

  private static final Logger logger = LoggerFactory.getLogger(BacktestExample.class);

  public static void main(String[] args) throws InterruptedException {
    var properties = loadPropertiesFromFile("invest.properties");
    var configuration = ConnectorConfiguration.loadFromProperties(properties);
    var factory = ServiceStubFactory.create(configuration);
    var streamFactory = StreamServiceStubFactory.create(factory);
    var strategyFactory = StrategyFactory.create(streamFactory);
    var executorService = Executors.newCachedThreadPool();

    logger.info("Start backtest");

    Function<BarSeries, Strategy> tradingStrategy = barSeries -> {
      ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
      EMAIndicator shortEma = new EMAIndicator(closePrice, 5);
      EMAIndicator longEma = new EMAIndicator(closePrice, 30);
      Rule buyingRule = new CrossedUpIndicatorRule(shortEma, longEma);
      Rule sellingRule = new CrossedDownIndicatorRule(shortEma, longEma);
      return new BaseStrategy(buyingRule, sellingRule);
    };

    var backtest = strategyFactory.newCandleStrategyBacktest(
      CandleStrategyBacktestConfiguration.builder()
        .setInstrumentId("e6123145-9665-43e0-8413-cd61b8aa9b13")
        .setCandleInterval(CandleInterval.CANDLE_INTERVAL_30_MIN)
        .setFrom(LocalDate.of(2018, 1, 15))
        .setTo(LocalDate.of(2025, 2, 5))
        .setTradeExecutionModel(new TradeOnCurrentCloseModel())
        .setTradeFeeModel(new LinearTransactionCostModel(0.003))
        .setExecutorService(executorService)
        .setStrategyAnalysis(barSeriesManager -> {
          AnalysisCriterion criterion = new ProfitCriterion();
          var barSeries = barSeriesManager.getBarSeries();
          var tradingRecord = barSeriesManager.run(tradingStrategy.apply(barSeries));
          var profit = criterion.calculate(barSeries, tradingRecord);
          logger.info("Profit: {}", profit);
        })
        .build());
    backtest.run();
    executorService.shutdownNow();
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
