package ru.ttech.piapi.springboot.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Properties;

@Getter
@Setter
@ConfigurationProperties(prefix = "invest.connector")
public class ConnectorProperties {

  private String token;
  private String appName = "tinkoff.invest-api-java-sdk";
  private String targetUrl = "invest-public-api.tinkoff.ru:443";
  private Sandbox sandbox = new Sandbox();
  private Connection connection = new Connection();
  private Grpc grpc = new Grpc();

  @Getter
  @Setter
  public static class Sandbox {
    private String targetUrl = "sandbox-invest-public-api.tinkoff.ru:443";
    private boolean enabled = false;
  }

  @Getter
  @Setter
  public static class Connection {
    private int timeout = 30000;
    private int keepalive = 60000;
    private Retry retry = new Retry();
    private int maxMessageSize = 16777216;

    @Getter
    @Setter
    public static class Retry {
      private int maxAttempts = 3;
      private int waitDuration = 2000;
    }
  }

  @Getter
  @Setter
  public static class Grpc {
    private boolean debug = false;
    private boolean contextFork = false;
  }

  public Properties toProperties() {
    Properties properties = new Properties();
    properties.setProperty("token", token);
    properties.setProperty("app.name", appName);
    properties.setProperty("target", targetUrl);
    properties.setProperty("sandbox.target", sandbox.getTargetUrl());
    properties.setProperty("sandbox.enabled", String.valueOf(sandbox.isEnabled()));
    properties.setProperty("connection.timeout", String.valueOf(connection.getTimeout()));
    properties.setProperty("connection.keepalive", String.valueOf(connection.getKeepalive()));
    properties.setProperty("connection.retry.max-attempts",
      String.valueOf(connection.getRetry().getMaxAttempts()));
    properties.setProperty("connection.retry.wait-duration",
      String.valueOf(connection.getRetry().getWaitDuration()));
    properties.setProperty("connection.max-message-size",
      String.valueOf(connection.getMaxMessageSize()));
    properties.setProperty("grpc.debug", String.valueOf(grpc.isDebug()));
    properties.setProperty("grpc.context-fork", String.valueOf(grpc.isContextFork()));
    return properties;
  }
}
