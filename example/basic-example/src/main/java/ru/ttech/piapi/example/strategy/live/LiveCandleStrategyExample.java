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
import ru.ttech.piapi.strategy.StrategyFactory;
import ru.ttech.piapi.strategy.candle.live.CandleStrategyConfiguration;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class LiveCandleStrategyExample {

  private static final Logger log = LoggerFactory.getLogger(LiveCandleStrategyExample.class);

  public static void main(String[] args) {
    var configuration = ConnectorConfiguration.loadPropertiesFromResources("invest.properties");
    var factory = ServiceStubFactory.create(configuration);
    var liveStrategy = new LiveCandleStrategyExample();
    var sandboxBalance = BigDecimal.valueOf(1_000_000);
    var instrumentLots = 1;
    var tradingService = new TradingServiceExample(factory, sandboxBalance, instrumentLots);
    int warmupLength = 100;

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
    var strategies = Map.of(
      ttechShare, createStrategy(5, 15),
      sberShare, createStrategy(10, 20),
      sberShareTwo, createStrategy(15, 25)
    );
    liveStrategy.startStrategy(factory, warmupLength, strategies, tradingService);
  }

  public void startStrategy(
    ServiceStubFactory serviceFactory, int warmupLength,
    Map<CandleInstrument, Function<BarSeries, Strategy>> strategies,
    TradingServiceExample tradingService
  ) {
    var streamFactory = StreamServiceStubFactory.create(serviceFactory);
    var streamManagerFactory = StreamManagerFactory.create(streamFactory);
    var executorService = Executors.newCachedThreadPool();
    var scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    var marketDataStreamManager = streamManagerFactory.newMarketDataStreamManager(executorService, scheduledExecutorService);
    var liveStrategyFactory = StrategyFactory.create(marketDataStreamManager);
    var strategy = liveStrategyFactory.newCandleStrategy(
      CandleStrategyConfiguration.builder()
        .setCandleSource(GetCandlesRequest.CandleSource.CANDLE_SOURCE_INCLUDE_WEEKEND)
        .setWarmupLength(warmupLength)
        .setStrategies(strategies)
        .setStrategyEnterAction(tradingService::onStrategyEnter)
        .setStrategyExitAction(tradingService::onStrategyExit)
        .build()
    );
    strategy.run();
    var hook = new Thread(() -> {
      log.info("Shutdown live trading...");
      marketDataStreamManager.shutdown();
    });
    Runtime.getRuntime().addShutdownHook(hook);
    try {
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      marketDataStreamManager.shutdown();
    }
  }

  public static Function<BarSeries, Strategy> createStrategy(int shortEma, int longEma) {
    return barSeries -> {
      ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
      SMAIndicator shortSma = new SMAIndicator(closePrice, shortEma);
      SMAIndicator longSma = new SMAIndicator(closePrice, longEma);
      Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma);
      Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma);
      return new BaseStrategy(buyingRule, sellingRule);
    };
  }
}
