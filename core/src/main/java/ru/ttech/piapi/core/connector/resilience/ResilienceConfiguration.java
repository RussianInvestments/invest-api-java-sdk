package ru.ttech.piapi.core.connector.resilience;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.grpc.MethodDescriptor;
import io.grpc.ServiceDescriptor;
import io.grpc.Status;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.exception.ServiceRuntimeException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

public class ResilienceConfiguration {

  private final ScheduledExecutorService scheduledExecutorService;
  private final RetryRegistry retryRegistry;

  public ResilienceConfiguration(ScheduledExecutorService scheduledExecutorService, RetryRegistry retryRegistry) {
    this.scheduledExecutorService = scheduledExecutorService;
    this.retryRegistry = retryRegistry;
  }

  public ScheduledExecutorService getScheduledExecutorService() {
    return scheduledExecutorService;
  }

  public Retry getRetryForMethod(MethodDescriptor<?, ?> method) {
    return retryRegistry.getConfiguration(method.getFullMethodName())
      .or(() -> Optional.ofNullable(method.getServiceName()).flatMap(retryRegistry::getConfiguration))
      .map(retryConfig -> retryRegistry.retry(method.getFullMethodName(), retryConfig))
      .orElseGet(() -> retryRegistry.retry(method.getFullMethodName()));
  }

  public static Builder builder(
    ScheduledExecutorService executorService,
    ConnectorConfiguration connectorConfiguration
  ) {
    return new Builder(executorService, connectorConfiguration);
  }

  public static class Builder {

    private static final String DEFAULT_CONFIG_NAME = "default";
    private final ScheduledExecutorService executorService;
    private final ConnectorConfiguration connectorConfiguration;
    private final Map<String, RetryConfig> retryConfigs = new HashMap<>();

    private Builder(ScheduledExecutorService executorService, ConnectorConfiguration connectorConfiguration) {
      this.executorService = executorService;
      this.connectorConfiguration = connectorConfiguration;
    }

    public Builder withDefaultRetryConfig(RetryConfig defaultRetryConfig) {
      if (retryConfigs.containsKey(DEFAULT_CONFIG_NAME)) {
        throw new IllegalArgumentException("Конфигурация по умолчанию уже существует!");
      }
      retryConfigs.put(DEFAULT_CONFIG_NAME, defaultRetryConfig);
      return this;
    }

    public Builder addServiceRetryConfig(ServiceDescriptor serviceDescriptor, RetryConfig retryConfig) {
      if (retryConfigs.containsKey(serviceDescriptor.getName())) {
        throw new IllegalArgumentException("Конфигурация для этого сервиса уже существует!");
      }
      retryConfigs.put(serviceDescriptor.getName(), retryConfig);
      return this;
    }

    public Builder addMethodRetryConfig(MethodDescriptor<?,?> method, RetryConfig retryConfig) {
      if (retryConfigs.containsKey(method.getFullMethodName())) {
        throw new IllegalArgumentException("Конфигурация для этого метода уже существует!");
      }
      retryConfigs.put(method.getFullMethodName(), retryConfig);
      return this;
    }

    public ResilienceConfiguration build() {
      if (!retryConfigs.containsKey(DEFAULT_CONFIG_NAME)) {
        retryConfigs.put(DEFAULT_CONFIG_NAME, createDefaultConfig(connectorConfiguration));
      }
      return new ResilienceConfiguration(executorService, RetryRegistry.of(retryConfigs));
    }

    private RetryConfig createDefaultConfig(ConnectorConfiguration configuration) {
      return RetryConfig.custom()
          .maxAttempts(configuration.getMaxAttempts())
          .intervalBiFunction((attempts, either) -> asResourceExhausted(either.getLeft())
            .map(exception -> {
              int rateLimitReset = exception.getRateLimitReset() * 1000;
              int waitDuration = rateLimitReset == 0 ? configuration.getWaitDuration() : rateLimitReset;
              return Duration.ofMillis(waitDuration).toMillis();
            })
            .orElseGet(() -> Duration.ofMillis(configuration.getWaitDuration()).toMillis()))
          .retryOnException(throwable -> {
            if (throwable instanceof ServiceRuntimeException) {
              var exception = (ServiceRuntimeException) throwable;
              Status status = exception.getErrorStatus();
              return status.getCode() == Status.Code.RESOURCE_EXHAUSTED || status.getCode() == Status.Code.UNAVAILABLE
                || status.getCode() == Status.Code.INTERNAL && exception.parseErrorCode() == 70001;
            }
            return false;
          }).build();
    }

    private Optional<ServiceRuntimeException> asResourceExhausted(Throwable throwable) {
      if (throwable instanceof ServiceRuntimeException
        && ((ServiceRuntimeException) throwable).getErrorType() == Status.Code.RESOURCE_EXHAUSTED) {
        return Optional.of((ServiceRuntimeException) throwable);
      }
      return Optional.empty();
    }
  }
}
