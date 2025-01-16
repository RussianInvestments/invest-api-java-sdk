package ru.tinkoff.piapi.core.connector;

import java.util.Properties;

public class ConnectorConfiguration {

  private final String token;
  private final String appName;
  private final String targetUrl;

  private ConnectorConfiguration(String token, String appName, String targetUrl) {
    this.token = token;
    this.appName = appName;
    this.targetUrl = targetUrl;
  }

  public static ConnectorConfiguration loadFromProperties(Properties properties) {
    String token = properties.getProperty("token", "");
    String appName = properties.getProperty("app.name", "");
    String targetUrl = properties.getProperty("target", "");
    return new ConnectorConfiguration(token, appName, targetUrl);
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
}
