package ru.ttech.piapi.example.strategy.backtest;

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
import ru.ttech.piapi.strategy.BacktestStrategyFactory;
import ru.ttech.piapi.strategy.candle.backtest.CandleStrategyBacktestConfiguration;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("DuplicatedCode")
public class ChooseBestStrategyExample {

  private static final Logger logger = LoggerFactory.getLogger(ChooseBestStrategyExample.class);

  public static void main(String[] args) {
    var configuration = ConnectorConfiguration.loadFromPropertiesFile("invest.properties");
    var backtestStrategyFactory = BacktestStrategyFactory.create(configuration);
    var executorService = Executors.newCachedThreadPool();

    List<Function<BarSeries, Strategy>> strategiesFunctions = IntStream.rangeClosed(2, 30)
      .mapToObj(longEmaPeriod -> createSimpleStrategy(5, longEmaPeriod))
      .collect(Collectors.toList());

    var backtest = backtestStrategyFactory.newCandleStrategyBacktest(
      CandleStrategyBacktestConfiguration.builder()
        .setInstrumentId("e6123145-9665-43e0-8413-cd61b8aa9b13")
        .setCandleInterval(CandleInterval.CANDLE_INTERVAL_30_MIN)
        .setFrom(LocalDate.of(2018, 1, 15))
        .setTo(LocalDate.of(2025, 2, 16))
        .setTradeExecutionModel(new TradeOnCurrentCloseModel())
        .setTradeFeeModel(new LinearTransactionCostModel(0.003))
        .setExecutorService(executorService)
        .setStrategyAnalysis(barSeriesManager -> {
          AnalysisCriterion criterion = new ProfitCriterion();
          var barSeries = barSeriesManager.getBarSeries();
          var strategies = strategiesFunctions.stream()
            .map(strategy -> strategy.apply(barSeries))
            .collect(Collectors.toList());
          var strategy = criterion.chooseBest(barSeriesManager, strategies);
          var tradingRecord = barSeriesManager.run(strategy);
          var bestProfit = criterion.calculate(barSeries, tradingRecord);
          logger.info("Best profit: {} with strategy: {}", bestProfit, strategy.getName());
        })
        .build());
    backtest.run();
    executorService.shutdownNow();
  }

  private static Function<BarSeries, Strategy> createSimpleStrategy(int shortEmaVal, int longEmaVal) {
    return barSeries -> {
      ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
      EMAIndicator shortEma = new EMAIndicator(closePrice, shortEmaVal);
      EMAIndicator longEma = new EMAIndicator(closePrice, longEmaVal);
      Rule buyingRule = new CrossedUpIndicatorRule(shortEma, longEma);
      Rule sellingRule = new CrossedDownIndicatorRule(shortEma, longEma);
      return new BaseStrategy(String.format("shortEma=%d, longEma=%d", shortEmaVal, longEmaVal), buyingRule, sellingRule);
    };
  }
}
