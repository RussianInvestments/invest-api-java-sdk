package ru.ttech.piapi.springboot.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.StreamManagerFactory;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.impl.marketdata.MarketDataStreamManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ConditionalOnClass(ConnectorConfiguration.class)
@EnableConfigurationProperties(ConnectorProperties.class)
@RequiredArgsConstructor
public class InvestAutoConfiguration {

  private final ConnectorProperties connectorProperties;

  @Bean
  @ConditionalOnMissingBean(ConnectorConfiguration.class)
  @ConditionalOnProperty(prefix = "invest.connector", name = "token")
  public ConnectorConfiguration connectorConfiguration() {
    return ConnectorConfiguration.loadFromProperties(connectorProperties.toProperties());
  }

  @Bean
  @ConditionalOnBean(ConnectorConfiguration.class)
  @ConditionalOnMissingBean(ServiceStubFactory.class)
  public ServiceStubFactory serviceStubFactory(ConnectorConfiguration connectorConfiguration) {
    return ServiceStubFactory.create(connectorConfiguration);
  }

  @Bean
  @ConditionalOnBean(ServiceStubFactory.class)
  @ConditionalOnMissingBean(StreamServiceStubFactory.class)
  public StreamServiceStubFactory streamServiceStubFactory(ServiceStubFactory serviceStubFactory) {
    return StreamServiceStubFactory.create(serviceStubFactory);
  }

  @Bean
  @ConditionalOnBean(StreamServiceStubFactory.class)
  @ConditionalOnMissingBean(StreamManagerFactory.class)
  public StreamManagerFactory streamManagerFactory(StreamServiceStubFactory streamServiceStubFactory) {
    return StreamManagerFactory.create(streamServiceStubFactory);
  }

  @Bean
  @ConditionalOnBean(StreamManagerFactory.class)
  @ConditionalOnMissingBean(MarketDataStreamManager.class)
  public MarketDataStreamManager marketDataStreamManager(
    StreamManagerFactory streamManagerFactory,
    @Qualifier("marketDataStreamManagerExecutorService") ExecutorService managerExecutorService,
    @Qualifier("streamHealthCheckScheduledExecutorService") ScheduledExecutorService scheduledExecutorService
  ) {
    return streamManagerFactory.newMarketDataStreamManager(managerExecutorService, scheduledExecutorService);
  }

  @Bean("marketDataStreamManagerExecutorService")
  public ExecutorService managerExecutorService() {
    return Executors.newCachedThreadPool();
  }

  @Bean("streamHealthCheckScheduledExecutorService")
  public ScheduledExecutorService scheduledExecutorService() {
    return Executors.newSingleThreadScheduledExecutor();
  }
}
