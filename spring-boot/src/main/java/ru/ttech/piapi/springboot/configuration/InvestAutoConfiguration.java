package ru.ttech.piapi.springboot.configuration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.springboot.bot.TradingBot;
import ru.ttech.piapi.springboot.bot.TradingBotInitializer;
import ru.ttech.piapi.strategy.StrategyFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(ConnectorProperties.class)
public class InvestAutoConfiguration {

  private final ConnectorProperties connectorProperties;

  public InvestAutoConfiguration(ConnectorProperties connectorProperties) {
    this.connectorProperties = connectorProperties;
  }

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
  public StrategyFactory strategyFactory(StreamServiceStubFactory streamServiceStubFactory) {
    return StrategyFactory.create(streamServiceStubFactory);
  }

  @Bean
  public ExecutorService tradingBotExecutorService() {
    return Executors.newCachedThreadPool();
  }

  @Bean
  public TradingBotInitializer tradingBotInitializer(
    ExecutorService tradingBotExecutorService,
    StrategyFactory strategyFactory,
    ObjectProvider<List<TradingBot>> tradingBots
  ) {
    return new TradingBotInitializer(
      tradingBotExecutorService,
      strategyFactory,
      tradingBots.getIfAvailable(Collections::emptyList)
    );
  }
}
