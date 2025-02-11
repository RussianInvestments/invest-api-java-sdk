package ru.ttech.piapi.springboot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import ru.ttech.piapi.strategy.StrategyFactory;
import ru.ttech.piapi.strategy.candle.live.CandleStrategyConfiguration;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
public class TradingBotInitializer implements InitializingBean {

  private final ExecutorService executorService;
  private final StrategyFactory strategyFactory;
  private final List<TradingBot> tradingBots;

  public TradingBotInitializer(
    ExecutorService executorService,
    StrategyFactory strategyFactory,
    List<TradingBot> tradingBots
  ) {
    this.executorService = executorService;
    this.strategyFactory = strategyFactory;
    this.tradingBots = tradingBots;
  }

  @Override
  public void afterPropertiesSet() {
    tradingBots.forEach(tradingBot -> executorService.submit(() -> {
      var strategy = strategyFactory.newCandleStrategy(
        CandleStrategyConfiguration.builder()
          .setInstrument(tradingBot.getInstrument())
          .setCandleSource(tradingBot.getCandleSource())
          .setWarmupLength(tradingBot.getWarmupLength())
          .setStrategy(tradingBot::getStrategy)
          .setStrategyEnterAction(tradingBot::onStrategyEnterAction)
          .setStrategyExitAction(tradingBot::onStrategyExitAction)
          .build());
      strategy.run();
      try {
        Thread.currentThread().join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } finally {
        strategy.shutdown();
      }
    }));
  }
}
