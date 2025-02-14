package ru.ttech.piapi.springboot.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Optional;
import java.util.Properties;

@Getter
@Setter
@ConfigurationProperties(prefix = "invest.connector")
public class ConnectorProperties {

  private String token;
  private String appName;
  private String targetUrl;
  private Sandbox sandbox = new Sandbox();
  private Connection connection = new Connection();
  private Grpc grpc = new Grpc();

  @Getter
  @Setter
  public static class Sandbox {
    private String targetUrl;
    private Boolean enabled;
  }

  @Getter
  @Setter
  public static class Connection {
    private Integer timeout;
    private Integer keepalive;
    private Retry retry = new Retry();
    private Integer maxMessageSize;

    @Getter
    @Setter
    public static class Retry {
      private Integer maxAttempts;
      private Integer waitDuration;
    }
  }

  @Getter
  @Setter
  public static class Grpc {
    private Boolean debug;
    private Boolean contextFork;
  }

  public Properties toProperties() {
    Properties properties = new Properties();
    Optional.ofNullable(token).ifPresent(token -> properties.setProperty("token", token));
    Optional.ofNullable(appName).ifPresent(appName -> properties.setProperty("app.name", appName));
    Optional.ofNullable(targetUrl).ifPresent(targetUrl -> properties.setProperty("target", targetUrl));
    Optional.ofNullable(sandbox.getTargetUrl())
      .ifPresent(targetUrl -> properties.setProperty("sandbox.target", targetUrl));
    Optional.ofNullable(sandbox.getEnabled())
      .ifPresent(enabled -> properties.setProperty("sandbox.enabled", String.valueOf(enabled)));
    Optional.ofNullable(connection.getTimeout())
      .ifPresent(timeout -> properties.setProperty("connection.timeout", String.valueOf(timeout)));
    Optional.ofNullable(connection.getKeepalive())
      .ifPresent(keepalive -> properties.setProperty("connection.keepalive", String.valueOf(keepalive)));
    Optional.ofNullable(connection.getRetry().getWaitDuration())
      .ifPresent(waitDuration -> properties.setProperty( "connection.retry.wait-duration", String.valueOf(waitDuration)));
    Optional.ofNullable(connection.getRetry().getMaxAttempts())
      .ifPresent(maxAttempts -> properties.setProperty("connection.retry.max-attempts", String.valueOf(maxAttempts)));
    Optional.ofNullable(connection.getMaxMessageSize())
      .ifPresent(maxMessageSize -> properties.setProperty("connection.max-message-size", String.valueOf(maxMessageSize)));
    Optional.ofNullable(grpc.getDebug())
      .ifPresent(debug -> properties.setProperty("grpc.debug", String.valueOf(debug)));
    Optional.ofNullable(grpc.getContextFork())
      .ifPresent(contextFork -> properties.setProperty("grpc.context-fork", String.valueOf(contextFork)));
    return properties;
  }
}
