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
    var configuration = ConnectorConfiguration.loadPropertiesFromResources("invest.properties");
    var chooseBestStrategyExample = new ChooseBestStrategyExample();
    String instrumentId = "e6123145-9665-43e0-8413-cd61b8aa9b13";
    LocalDate from = LocalDate.of(2024, 1, 15);
    LocalDate to = LocalDate.of(2025, 2, 16);
    double commissionFee = 0.003;
    CandleInterval candleInterval = CandleInterval.CANDLE_INTERVAL_30_MIN;
    int shortEmaStart = 10;
    int shortEmaEnd = 15;
    int longEmaStart = 10;
    int longEmaEnd = 15;
    chooseBestStrategyExample.startBacktest(configuration, instrumentId, candleInterval, from, to,
      shortEmaStart, shortEmaEnd, longEmaStart, longEmaEnd, commissionFee);
  }

  public void startBacktest(
    ConnectorConfiguration configuration,
    String instrumentId, CandleInterval candleInterval,
    LocalDate from, LocalDate to,
    int shortEmaStart, int shortEmaEnd,
    int longEmaStart, int longEmaEnd,
    double commissionFee
  ) {
    var backtestStrategyFactory = BacktestStrategyFactory.create(configuration);
    var executorService = Executors.newCachedThreadPool();
    List<Function<BarSeries, Strategy>> strategiesFunctions =
      IntStream.rangeClosed(longEmaStart, longEmaEnd)
        .boxed()
        .flatMap(longEmaPeriod ->
          IntStream.rangeClosed(shortEmaStart, shortEmaEnd)
            .mapToObj(shortEmaPeriod -> createSimpleStrategy(shortEmaPeriod, longEmaPeriod))
        )
        .collect(Collectors.toList());

    var backtest = backtestStrategyFactory.newCandleStrategyBacktest(
      CandleStrategyBacktestConfiguration.builder()
        .setInstrumentId(instrumentId)
        .setCandleInterval(candleInterval)
        .setFrom(from)
        .setTo(to)
        .setTradeExecutionModel(new TradeOnCurrentCloseModel())
        .setTradeFeeModel(new LinearTransactionCostModel(commissionFee))
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
    executorService.shutdown();
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
