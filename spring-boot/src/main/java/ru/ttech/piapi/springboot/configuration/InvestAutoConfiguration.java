package ru.ttech.piapi.springboot.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.StreamManagerFactory;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.impl.marketdata.MarketDataStreamManager;
import ru.ttech.piapi.springboot.bot.CandleTradingBot;
import ru.ttech.piapi.springboot.bot.TradingBotInitializer;
import ru.ttech.piapi.strategy.StrategyFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(ConnectorProperties.class)
@RequiredArgsConstructor
public class InvestAutoConfiguration {

  private final ConnectorProperties connectorProperties;

  @Bean
  @ConditionalOnProperty(prefix = "invest.connector", name = "token")
  public ConnectorConfiguration connectorConfiguration() {
    return ConnectorConfiguration.loadFromProperties(connectorProperties.toProperties());
  }

  @Bean
  public ServiceStubFactory serviceStubFactory(ConnectorConfiguration connectorConfiguration) {
    return ServiceStubFactory.create(connectorConfiguration);
  }

  @Bean
  public StreamServiceStubFactory streamServiceStubFactory(ServiceStubFactory serviceStubFactory) {
    return StreamServiceStubFactory.create(serviceStubFactory);
  }

  @Bean
  public StreamManagerFactory streamManagerFactory(StreamServiceStubFactory streamServiceStubFactory) {
    return StreamManagerFactory.create(streamServiceStubFactory);
  }

  @Bean
  public MarketDataStreamManager marketDataStreamManager(StreamManagerFactory streamManagerFactory) {
    return streamManagerFactory.newMarketDataStreamManager();
  }

  @Bean
  public StrategyFactory strategyFactory(MarketDataStreamManager marketDataStreamManager) {
    return StrategyFactory.create(marketDataStreamManager);
  }

  @Bean
  public ExecutorService tradingBotExecutorService() {
    return Executors.newCachedThreadPool();
  }

  @Bean
  public TradingBotInitializer tradingBotInitializer(
    ExecutorService tradingBotExecutorService,
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
