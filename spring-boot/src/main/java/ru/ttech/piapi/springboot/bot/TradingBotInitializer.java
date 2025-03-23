package ru.ttech.piapi.springboot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import ru.ttech.piapi.strategy.StrategyFactory;
import ru.ttech.piapi.strategy.candle.live.CandleStrategy;
import ru.ttech.piapi.strategy.candle.live.CandleStrategyConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
public class TradingBotInitializer implements InitializingBean {

  private final ExecutorService executorService;
  private final StrategyFactory strategyFactory;
  private final List<CandleTradingBot> candleTradingBots;
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final List<CandleStrategy> strategies = new ArrayList<>();

  public TradingBotInitializer(
    ExecutorService executorService,
    StrategyFactory strategyFactory,
    List<CandleTradingBot> candleTradingBots
  ) {
    this.executorService = executorService;
    this.strategyFactory = strategyFactory;
    this.candleTradingBots = candleTradingBots;
  }

  @Override
  public void afterPropertiesSet() {
    candleTradingBots.forEach(tradingBot -> executorService.submit(() -> {
      var strategy = strategyFactory.newCandleStrategy(
        CandleStrategyConfiguration.builder()
          .setStrategies(tradingBot.getStrategies())
          .setCandleSource(tradingBot.getCandleSource())
          .setWarmupLength(tradingBot.getWarmupLength())
          .setStrategyEnterAction(tradingBot::onStrategyEnterAction)
          .setStrategyExitAction(tradingBot::onStrategyExitAction)
          .build());
      strategy.run();
      strategies.add(strategy);
    }));
  }
}
