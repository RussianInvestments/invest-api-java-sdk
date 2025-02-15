package ru.ttech.piapi.core.connector;

import java.util.Properties;

public class ConnectorConfiguration {

  private static final String TOKEN_PROPERTY_KEY = "token";
  private static final String APP_NAME_PROPERTY_NAME = "app.name";
  private static final String TARGET_PROPERTY_NAME = "target";
  private static final String SANDBOX_TARGET_PROPERTY_NAME = "sandbox.target";
  private static final String SANDBOX_ENABLED_PROPERTY_NAME = "sandbox.enabled";
  private static final String TIMEOUT_PROPERTY_NAME = "connection.timeout";
  private static final String KEEPALIVE_PROPERTY_NAME = "connection.keepalive";
  private static final String MAX_ATTEMPTS_PROPERTY_NAME = "connection.retry.max-attempts";
  private static final String WAIT_DURATION_PROPERTY_NAME = "connection.retry.wait-duration";
  private static final String MAX_INBOUND_MESSAGE_SIZE_PROPERTY_NAME = "connection.max-message-size";
  private static final String GRPC_DEBUG_PROPERTY_NAME = "grpc.debug";
  private static final String GRPC_CONTEXT_FORK_PROPERTY_NAME = "grpc.context-fork";
  private static final String MARKET_DATA_MAX_STREAMS_COUNT = "stream.market-data.max-streams-count";
  private static final String MARKET_DATA_MAX_SUBSCRIPTIONS_COUNT = "stream.market-data.max-subscriptions-count";
  private static final String DEFAULT_TARGET = "invest-public-api.tinkoff.ru:443";
  private static final String DEFAULT_SANDBOX_TARGET = "sandbox-invest-public-api.tinkoff.ru:443";
  private static final String DEFAULT_SANDBOX_ENABLED = "false";
  private static final String DEFAULT_APP_NAME = "tinkoff.invest-api-java-sdk";
  private static final String DEFAULT_TIMEOUT = "30000";
  private static final String DEFAULT_KEEPALIVE = "60000";
  private static final String DEFAULT_MAX_ATTEMPTS = "3";
  private static final String DEFAULT_WAIT_DURATION = "2000";
  private static final String DEFAULT_MAX_INBOUND_MESSAGE_SIZE = "16777216";
  private static final String DEFAULT_GRPC_DEBUG = "false";
  private static final String DEFAULT_GRPC_CONTEXT_FORK = "false";
  private static final String DEFAULT_MARKET_DATA_MAX_STREAMS_COUNT = "16";
  private static final String DEFAULT_MARKET_DATA_MAX_SUBSCRIPTIONS_COUNT = "2";

  private final String token;
  private final String appName;
  private final String targetUrl;
  private final String sandboxTargetUrl;
  private final boolean sandboxEnabled;
  private final int timeout;
  private final int keepalive;
  private final int maxAttempts;
  private final int waitDuration;
  private final int maxInboundMessageSize;
  private final boolean grpcDebug;
  private final boolean grpcContextFork;
  private final int maxMarketDataStreamsCount;
  private final int maxMarketDataSubscriptionsCount;

  private ConnectorConfiguration(String token, String appName, String targetUrl, String sandboxTargetUrl,
                                 boolean sandboxEnabled, int timeout, int keepalive, int maxAttempts, int waitDuration,
                                 int maxInboundMessageSize, boolean grpcDebug, boolean grpcContextFork,
                                 int maxMarketDataStreamsCount, int maxMarketDataSubscriptionsCount) {
    this.token = token;
    this.appName = appName;
    this.targetUrl = targetUrl;
    this.sandboxTargetUrl = sandboxTargetUrl;
    this.sandboxEnabled = sandboxEnabled;
    this.timeout = timeout;
    this.keepalive = keepalive;
    this.maxAttempts = maxAttempts;
    this.waitDuration = waitDuration;
    this.maxInboundMessageSize = maxInboundMessageSize;
    this.grpcDebug = grpcDebug;
    this.grpcContextFork = grpcContextFork;
    this.maxMarketDataStreamsCount = maxMarketDataStreamsCount;
    this.maxMarketDataSubscriptionsCount = maxMarketDataSubscriptionsCount;
  }

  public static ConnectorConfiguration loadFromProperties(Properties properties) {
    String token = properties.getProperty(TOKEN_PROPERTY_KEY);
    if (token == null) {
      throw new IllegalArgumentException("Токен должен быть указан!");
    }
    String appName = properties.getProperty(APP_NAME_PROPERTY_NAME, DEFAULT_APP_NAME);
    String targetUrl = properties.getProperty(TARGET_PROPERTY_NAME, DEFAULT_TARGET);
    String sandboxTargetUrl = properties.getProperty(SANDBOX_TARGET_PROPERTY_NAME, DEFAULT_SANDBOX_TARGET);
    boolean sandboxEnabled = Boolean.parseBoolean(
      properties.getProperty(SANDBOX_ENABLED_PROPERTY_NAME, DEFAULT_SANDBOX_ENABLED));
    int timeout = Integer.parseInt(properties.getProperty(TIMEOUT_PROPERTY_NAME, DEFAULT_TIMEOUT));
    int keepalive = Integer.parseInt(properties.getProperty(KEEPALIVE_PROPERTY_NAME, DEFAULT_KEEPALIVE));
    int maxAttempts = Integer.parseInt(properties.getProperty(MAX_ATTEMPTS_PROPERTY_NAME, DEFAULT_MAX_ATTEMPTS));
    int waitDuration = Integer.parseInt(properties.getProperty(WAIT_DURATION_PROPERTY_NAME, DEFAULT_WAIT_DURATION));
    int maxInboundMessageSize = Integer.parseInt(
      properties.getProperty(MAX_INBOUND_MESSAGE_SIZE_PROPERTY_NAME, DEFAULT_MAX_INBOUND_MESSAGE_SIZE));
    boolean grpcDebug = Boolean.parseBoolean(properties.getProperty(GRPC_DEBUG_PROPERTY_NAME, DEFAULT_GRPC_DEBUG));
    boolean grpcContextFork = Boolean.parseBoolean(
      properties.getProperty(GRPC_CONTEXT_FORK_PROPERTY_NAME, DEFAULT_GRPC_CONTEXT_FORK));
    int maxMarketDataStreamsCount = Integer.parseInt(
      properties.getProperty(MARKET_DATA_MAX_STREAMS_COUNT, DEFAULT_MARKET_DATA_MAX_STREAMS_COUNT));
    int maxMarketDataSubscriptionsCount = Integer.parseInt(
      properties.getProperty(MARKET_DATA_MAX_SUBSCRIPTIONS_COUNT, DEFAULT_MARKET_DATA_MAX_SUBSCRIPTIONS_COUNT));
    return new ConnectorConfiguration(
      token, appName, targetUrl, sandboxTargetUrl, sandboxEnabled, timeout, keepalive, maxAttempts, waitDuration,
      maxInboundMessageSize, grpcDebug, grpcContextFork, maxMarketDataStreamsCount, maxMarketDataSubscriptionsCount
    );
  }

  public String getToken() {
    return token;
  }

  public String getAppName() {
    return appName;
  }

  public String getTargetUrl() {
    return targetUrl;
  }

  public String getSandboxTargetUrl() {
    return sandboxTargetUrl;
  }

  public int getTimeout() {
    return timeout;
  }

  public int getKeepalive() {
    return keepalive;
  }

  public int getMaxInboundMessageSize() {
    return maxInboundMessageSize;
  }

  public boolean isGrpcDebug() {
    return grpcDebug;
  }

  public boolean isGrpcContextFork() {
    return grpcContextFork;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public int getWaitDuration() {
    return waitDuration;
  }

  public boolean isSandboxEnabled() {
    return sandboxEnabled;
  }

  public int getMaxMarketDataStreamsCount() {
    return maxMarketDataStreamsCount;
  }

  public int getMaxMarketDataSubscriptionsCount() {
    return maxMarketDataSubscriptionsCount;
  }
}
