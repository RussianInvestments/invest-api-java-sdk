package ru.ttech.piapi.core.connector.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
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

/**
 * Конфигурация resilience для обёрток
 *
 * <p>Пример создания конфигурации: <pre>{@code
 *     var configuration = ResilienceConfiguration.builder(executorService, configuration)
 *       // создаём две конфигурации retry, на сервис и на метод. Конфигурация на метод считается приоритетной
 *       .withRetryForMethod(
 *         MarketDataServiceGrpc.getGetLastPricesMethod(),
 *         RetryConfig.custom().waitDuration(Duration.ofMillis(100)).maxAttempts(5).build())
 *       .withRetryForService(
 *         MarketDataServiceGrpc.getServiceDescriptor(),
 *         RetryConfig.custom().waitDuration(Duration.ofMillis(100)).maxAttempts(3).build())
 *       .build();
 * }</pre>
 */
public class ResilienceConfiguration {

  private final ScheduledExecutorService executorService;
  private final RetryRegistry retryRegistry;
  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final RateLimiterRegistry rateLimiterRegistry;

  private ResilienceConfiguration(
    ScheduledExecutorService scheduledExecutorService,
    RetryRegistry retryRegistry,
    CircuitBreakerRegistry circuitBreakerRegistry,
    RateLimiterRegistry rateLimiterRegistry
  ) {
    this.executorService = scheduledExecutorService;
    this.retryRegistry = retryRegistry;
    this.circuitBreakerRegistry = circuitBreakerRegistry;
    this.rateLimiterRegistry = rateLimiterRegistry;
  }

  public ScheduledExecutorService getExecutorService() {
    return executorService;
  }

  public Retry getRetryForMethod(MethodDescriptor<?, ?> method) {
    String methodFullName = method.getFullMethodName();
    return retryRegistry.getConfiguration(method.getFullMethodName())
      .or(() -> Optional.ofNullable(method.getServiceName()).flatMap(retryRegistry::getConfiguration))
      .map(retryConfig -> retryRegistry.retry(methodFullName, retryConfig))
      .orElseGet(() -> retryRegistry.retry(methodFullName));
  }

  public CircuitBreaker getCircuitBreakerForMethod(MethodDescriptor<?, ?> method) {
    String methodFullName = method.getFullMethodName();
    return circuitBreakerRegistry.getConfiguration(method.getFullMethodName())
      .or(() -> Optional.ofNullable(method.getServiceName()).flatMap(circuitBreakerRegistry::getConfiguration))
      .map(circuitBreakerConfig -> circuitBreakerRegistry.circuitBreaker(methodFullName, circuitBreakerConfig))
      .orElseGet(() -> circuitBreakerRegistry.circuitBreaker(methodFullName));
  }

  public RateLimiter getRateLimiterForMethod(MethodDescriptor<?, ?> method) {
    String methodFullName = method.getFullMethodName();
    return rateLimiterRegistry.getConfiguration(method.getFullMethodName())
      .or(() -> Optional.ofNullable(method.getServiceName()).flatMap(rateLimiterRegistry::getConfiguration))
      .map(rateLimiterConfig -> rateLimiterRegistry.rateLimiter(methodFullName, rateLimiterConfig))
      .orElseGet(() -> rateLimiterRegistry.rateLimiter(methodFullName));
  }

  /**
   * Метод получения билдера для создания конфигурации
   *
   * @param executorService Пул потоков. Требуется для resilience асинхронных операций
   * @param connectorConfiguration Конфигурация клиента
   * @return Builder
   */
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
    private final Map<String, CircuitBreakerConfig> circuitBreakerConfigs = new HashMap<>();
    private final Map<String, RateLimiterConfig> rateLimiterConfigs = new HashMap<>();

    private Builder(ScheduledExecutorService executorService, ConnectorConfiguration connectorConfiguration) {
      this.executorService = executorService;
      this.connectorConfiguration = connectorConfiguration;
    }

    /**
     * Установка конфигрурации Retry по умолчанию
     *
     * @param defaultRetryConfig Конфигурация Retry
     * @return Builder
     */
    public Builder withDefaultRetry(RetryConfig defaultRetryConfig) {
      return addConfigToMap(retryConfigs, DEFAULT_CONFIG_NAME, defaultRetryConfig);
    }

    /**
     * Добавление конфигрурации Retry для сервиса
     *
     * @param serviceDescriptor gRPC сервис
     * @param retryConfig Конфигурация Retry
     * @return Builder
     */
    public Builder withRetryForService(ServiceDescriptor serviceDescriptor, RetryConfig retryConfig) {
      return addConfigToMap(retryConfigs, serviceDescriptor.getName(), retryConfig);
    }

    /**
     * Добавление конфигрурации Retry для метода в сервисе
     *
     * @param method Метод gRPC сервиса
     * @param retryConfig Конфигурация Retry
     * @return Builder
     */
    public Builder withRetryForMethod(MethodDescriptor<?, ?> method, RetryConfig retryConfig) {
      return addConfigToMap(retryConfigs, method.getFullMethodName(), retryConfig);
    }

    /**
     * Установка конфигрурации CircuitBreaker по умолчанию
     *
     * @param defaultCircuitBreakerConfig Конфигурация CircuitBreaker
     * @return Builder
     */
    public Builder withDefaultCircuitBreaker(CircuitBreakerConfig defaultCircuitBreakerConfig) {
      return addConfigToMap(circuitBreakerConfigs, DEFAULT_CONFIG_NAME, defaultCircuitBreakerConfig);
    }

    /**
     * Добавление конфигрурации CircuitBreaker для сервиса
     *
     * @param serviceDescriptor gRPC сервис
     * @param circuitBreakerConfig Конфигурация CircuitBreaker
     * @return Builder
     */
    public Builder withCircuitBreakerForService(
      ServiceDescriptor serviceDescriptor,
      CircuitBreakerConfig circuitBreakerConfig
    ) {
      return addConfigToMap(circuitBreakerConfigs, serviceDescriptor.getName(), circuitBreakerConfig);
    }

    /**
     * Добавление конфигрурации CircuitBreaker для метода в сервисе
     *
     * @param method Метод gRPC сервиса
     * @param circuitBreakerConfig Конфигурация CircuitBreaker
     * @return Builder
     */
    public Builder withCircuitBreakerForMethod(
      MethodDescriptor<?, ?> method,
      CircuitBreakerConfig circuitBreakerConfig
    ) {
      return addConfigToMap(circuitBreakerConfigs, method.getFullMethodName(), circuitBreakerConfig);
    }

    /**
     * Установка конфигрурации RateLimiter по умолчанию
     *
     * @param defaultRateLimiterConfig Конфигурация RateLimiter
     * @return Builder
     */
    public Builder withDefaultRateLimiter(RateLimiterConfig defaultRateLimiterConfig) {
      return addConfigToMap(rateLimiterConfigs, DEFAULT_CONFIG_NAME, defaultRateLimiterConfig);
    }

    /**
     * Добавление конфигрурации RateLimiter для сервиса
     *
     * @param serviceDescriptor gRPC сервис
     * @param rateLimiterConfig Конфигурация RateLimiter
     * @return Builder
     */
    public Builder withRateLimiterForService(ServiceDescriptor serviceDescriptor, RateLimiterConfig rateLimiterConfig) {
      return addConfigToMap(rateLimiterConfigs, serviceDescriptor.getName(), rateLimiterConfig);
    }

    /**
     * Добавление конфигрурации RateLimiter для  метода в сервисе
     *
     * @param method Метод gRPC сервиса
     * @param rateLimiterConfig Конфигурация RateLimiter
     * @return Builder
     */
    public Builder withRateLimiterForMethod(MethodDescriptor<?, ?> method, RateLimiterConfig rateLimiterConfig) {
      return addConfigToMap(rateLimiterConfigs, method.getFullMethodName(), rateLimiterConfig);
    }

    /**
     * Завершает создание конфигурации resilience
     *
     * @return Конфигурация resilience
     */
    public ResilienceConfiguration build() {
      if (!retryConfigs.containsKey(DEFAULT_CONFIG_NAME)) {
        retryConfigs.put(DEFAULT_CONFIG_NAME, createDefaultRetryConfig(connectorConfiguration));
      }
      if (!circuitBreakerConfigs.containsKey(DEFAULT_CONFIG_NAME)) {
        circuitBreakerConfigs.put(DEFAULT_CONFIG_NAME, createDefaultCircuitBreakerConfig(connectorConfiguration));
      }
      if (!rateLimiterConfigs.containsKey(DEFAULT_CONFIG_NAME)) {
        rateLimiterConfigs.put(DEFAULT_CONFIG_NAME, createDefaultRateLimiterConfig(connectorConfiguration));
      }
      return new ResilienceConfiguration(
        executorService,
        RetryRegistry.of(retryConfigs),
        CircuitBreakerRegistry.of(circuitBreakerConfigs),
        RateLimiterRegistry.of(rateLimiterConfigs)
      );
    }

    private <T> Builder addConfigToMap(Map<String, T> configMap, String key, T config) {
      if (configMap.containsKey(key)) {
        throw new IllegalArgumentException(
          String.format("Конфигурация %s для %s уже существует!", config.getClass().getName(), key)
        );
      }
      configMap.put(key, config);
      return this;
    }

    private RetryConfig createDefaultRetryConfig(ConnectorConfiguration configuration) {
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

    private CircuitBreakerConfig createDefaultCircuitBreakerConfig(ConnectorConfiguration configuration) {
      // TODO: создать конфиг по умолчанию
      return CircuitBreakerConfig.ofDefaults();
    }

    private RateLimiterConfig createDefaultRateLimiterConfig(ConnectorConfiguration configuration) {
      // TODO: создать конфиг по умолчанию
      return RateLimiterConfig.ofDefaults();
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
