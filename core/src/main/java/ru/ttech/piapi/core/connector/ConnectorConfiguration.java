package ru.ttech.piapi.core.connector;

import java.util.Properties;

public class ConnectorConfiguration {

  private static final String TOKEN_PROPERTY_KEY = "token";
  private static final String APP_NAME_PROPERTY_NAME = "app.name";
  private static final String TARGET_PROPERTY_NAME = "target";
  private static final String SANDBOX_TARGET_PROPERTY_NAME = "sandbox-target";
  private static final String TIMEOUT_PROPERTY_NAME = "connection.timeout";
  private static final String KEEPALIVE_PROPERTY_NAME = "connection.keepalive";
  private static final String MAX_INBOUND_MESSAGE_SIZE_PROPERTY_NAME = "connection.max-message-size";
  private static final String GRPC_DEBUG_PROPERTY_NAME = "grpc.debug";
  private static final String DEFAULT_TARGET = "invest-public-api.tinkoff.ru:443";
  private static final String DEFAULT_SANDBOX_TARGET = "sandbox-invest-public-api.tinkoff.ru:443";
  private static final String DEFAULT_APP_NAME = "tinkoff.invest-api-java-sdk";
  private static final String DEFAULT_TIMEOUT = "30000";
  private static final String DEFAULT_KEEPALIVE = "60000";
  private static final String DEFAULT_MAX_INBOUND_MESSAGE_SIZE = "16777216";
  private static final String DEFAULT_GRPC_DEBUG = "false";

  private final String token;
  private final String appName;
  private final String targetUrl;
  private final String sandboxTargetUrl;
  private final int timeout;
  private final int keepalive;
  private final int maxInboundMessageSize;
  private final boolean grpcDebug;

  private ConnectorConfiguration(String token, String appName, String targetUrl, String sandboxTargetUrl,
                                 int timeout, int keepalive, int maxInboundMessageSize, boolean grpcDebug) {
    this.token = token;
    this.appName = appName;
    this.targetUrl = targetUrl;
    this.sandboxTargetUrl = sandboxTargetUrl;
    this.timeout = timeout;
    this.keepalive = keepalive;
    this.maxInboundMessageSize = maxInboundMessageSize;
    this.grpcDebug = grpcDebug;
  }

  public static ConnectorConfiguration loadFromProperties(Properties properties) {
    String token = properties.getProperty(TOKEN_PROPERTY_KEY);
    if (token == null) {
      throw new IllegalArgumentException("Токен должен быть указан!");
    }
    String appName = properties.getProperty(APP_NAME_PROPERTY_NAME, DEFAULT_APP_NAME);
    String targetUrl = properties.getProperty(TARGET_PROPERTY_NAME, DEFAULT_TARGET);
    String sandboxTargetUrl = properties.getProperty(SANDBOX_TARGET_PROPERTY_NAME, DEFAULT_SANDBOX_TARGET);
    int timeout = Integer.parseInt(properties.getProperty(TIMEOUT_PROPERTY_NAME, DEFAULT_TIMEOUT));
    int keepalive = Integer.parseInt(properties.getProperty(KEEPALIVE_PROPERTY_NAME, DEFAULT_KEEPALIVE));
    int maxInboundMessageSize = Integer.parseInt(
      properties.getProperty(MAX_INBOUND_MESSAGE_SIZE_PROPERTY_NAME, DEFAULT_MAX_INBOUND_MESSAGE_SIZE));
    boolean grpcDebug = Boolean.parseBoolean(properties.getProperty(GRPC_DEBUG_PROPERTY_NAME, DEFAULT_GRPC_DEBUG));
    return new ConnectorConfiguration(
      token, appName, targetUrl, sandboxTargetUrl, timeout, keepalive, maxInboundMessageSize, grpcDebug
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
}
