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

  /**
   * <a href="https://developer.tbank.ru/invest/intro/intro/token">Токен</a>
   *  доступа к API Т-Инвестиций
   */
  private String token;
  /**
   * Имя Вашего приложения
   */
  private String appName;
  /**
   * URL API Т-Инвестиций
   */
  private String targetUrl;
  /**
   * Настройки песочницы
   */
  private Sandbox sandbox = new Sandbox();
  /**
   * Настройки соединения
   */
  private Connection connection = new Connection();
  /**
   * Настройки gRPC клиента
   */
  private Grpc grpc = new Grpc();
  /**
   * Настройки stream-соединений
   */
  private Stream stream = new Stream();

  @Getter
  @Setter
  public static class Sandbox {
    /**
     * URL API песочницы Т-Инвестиций
     */
    private String targetUrl;
    /**
     * Переключение на песочницу
     */
    private Boolean enabled;
  }

  @Getter
  @Setter
  public static class Connection {
    /**
     * Таймаут соединения
     */
    private Integer timeout;
    /**
     * Интервал проверки соединения
     */
    private Integer keepalive;
    /**
     * Настройки повторных запросов при ошибках сервера или соединения
     */
    private Retry retry = new Retry();
    /**
     * Максимальный размер сообщения
     */
    private Integer maxMessageSize;

    @Getter
    @Setter
    public static class Retry {
      /**
       * Максимальное количество попыток отправки запроса
       */
      private Integer maxAttempts;
      /**
       * Интервал ожидания между попытками отправки запроса
       */
      private Integer waitDuration;
    }
  }

  @Getter
  @Setter
  public static class Grpc {
    /**
     * Включение отладочной информации
     */
    private Boolean debug;
    /**
     * Включение форка контекста
     */
    private Boolean contextFork;
  }

  @Getter
  @Setter
  public static class Stream {

    /**
     * Интервал пинга в стриме в миллисекундах
     */
    private Integer pingDelay;
    /**
     * Таймаут отсутствия сообщений в стриме в миллисекундах
     */
    private Integer inactivityTimeout;
    /**
     * Настройка MarketDataStream
     */
    private MarketData marketData = new MarketData();

    @Getter
    @Setter
    public static class MarketData {
      /**
       * Максимальное число одновременно открытых стримов для MarketData
       */
      private Integer maxStreamsCount;
      /**
       * Максимальное число одновременных подписок в одном стриме
       */
      private Integer maxSubscriptionsCount;
    }
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
    Optional.ofNullable(stream.getMarketData().getMaxStreamsCount())
      .ifPresent(maxStreamsCount -> properties.setProperty("stream.market-data.max-streams-count", String.valueOf(maxStreamsCount)));
    Optional.ofNullable(stream.getMarketData().getMaxSubscriptionsCount())
      .ifPresent(maxSubscriptionsCount -> properties.setProperty("stream.market-data.max-subscriptions-count", String.valueOf(maxSubscriptionsCount)));
    Optional.ofNullable(stream.getPingDelay())
      .ifPresent(pingDelay -> properties.setProperty("stream.ping-delay", String.valueOf(pingDelay)));
    Optional.ofNullable(stream.getInactivityTimeout())
      .ifPresent(inactivityTimeout -> properties.setProperty("stream.inactivity-timeout", String.valueOf(inactivityTimeout)));
    return properties;
  }
}
