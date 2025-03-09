package ru.ttech.piapi.springboot.configuration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.ttech.piapi.core.impl.marketdata.MarketDataStreamManager;
import ru.ttech.piapi.springboot.bot.CandleTradingBot;
import ru.ttech.piapi.springboot.bot.TradingBotInitializer;
import ru.ttech.piapi.strategy.StrategyFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@ConditionalOnClass(StrategyFactory.class)
public class StrategyAutoConfiguration {

  @Bean("tradingBotExecutorService")
  public ExecutorService tradingBotExecutorService() {
    return Executors.newSingleThreadExecutor();
  }

  @Bean
  @ConditionalOnBean(MarketDataStreamManager.class)
  @ConditionalOnMissingBean(StrategyFactory.class)
  public StrategyFactory strategyFactory(MarketDataStreamManager marketDataStreamManager) {
    return StrategyFactory.create(marketDataStreamManager);
  }

  @Bean
  @ConditionalOnBean(StrategyFactory.class)
  public TradingBotInitializer tradingBotInitializer(
    @Qualifier("tradingBotExecutorService") ExecutorService tradingBotExecutorService,
    StrategyFactory strategyFactory,
    ObjectProvider<List<CandleTradingBot>> candleTradingBots
  ) {
    return new TradingBotInitializer(
      tradingBotExecutorService,
      strategyFactory,
      candleTradingBots.getIfAvailable(Collections::emptyList)
    );
  }
}
