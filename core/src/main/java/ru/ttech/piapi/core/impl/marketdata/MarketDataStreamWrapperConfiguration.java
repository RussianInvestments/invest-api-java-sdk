package ru.ttech.piapi.core.impl.marketdata;

import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
import ru.ttech.piapi.core.impl.marketdata.wrapper.CandleWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.LastPriceWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.OrderBookWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.TradeWrapper;
import ru.ttech.piapi.core.impl.marketdata.wrapper.TradingStatusWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Конфигурация для обёртки {@link MarketDataStreamWrapper}
 */
public class MarketDataStreamWrapperConfiguration {

  private final ScheduledExecutorService executorService;
  private final List<OnNextListener<CandleWrapper>> onCandleListeners;
  private final List<OnNextListener<LastPriceWrapper>> onLastPriceListeners;
  private final List<OnNextListener<OrderBookWrapper>> onOrderBookListeners;
  private final List<OnNextListener<TradeWrapper>> onTradeListeners;
  private final List<OnNextListener<TradingStatusWrapper>> onTradingStatusListeners;
  private final List<Runnable> onConnectListeners;

  public MarketDataStreamWrapperConfiguration(
    ScheduledExecutorService executorService,
    List<OnNextListener<CandleWrapper>> onCandleListeners,
    List<OnNextListener<LastPriceWrapper>> onLastPriceListeners,
    List<OnNextListener<OrderBookWrapper>> onOrderBookListeners,
    List<OnNextListener<TradeWrapper>> onTradeListeners,
    List<OnNextListener<TradingStatusWrapper>> onTradingStatusListeners,
    List<Runnable> onConnectListeners
  ) {
    this.executorService = executorService;
    this.onCandleListeners = onCandleListeners;
    this.onLastPriceListeners = onLastPriceListeners;
    this.onOrderBookListeners = onOrderBookListeners;
    this.onTradeListeners = onTradeListeners;
    this.onTradingStatusListeners = onTradingStatusListeners;
    this.onConnectListeners = onConnectListeners;
  }

  public ScheduledExecutorService getExecutorService() {
    return executorService;
  }

  public List<Runnable> getOnConnectListeners() {
    return onConnectListeners;
  }

  public MarketDataStreamConfiguration.Builder getStreamWrapperConfigBuilder() {
    var builder = MarketDataStreamConfiguration.builder();
    onCandleListeners.forEach(builder::addOnCandleListener);
    onLastPriceListeners.forEach(builder::addOnLastPriceListener);
    onOrderBookListeners.forEach(builder::addOnOrderBookListener);
    onTradeListeners.forEach(builder::addOnTradeListener);
    onTradingStatusListeners.forEach(builder::addOnTradingStatusListener);
    return builder;
  }

  /**
   *
   *
   * @param executorService поток для проверки состояния соединения
   * @return Билдер конфигурации обёртки над стримом
   */
  public static Builder builder(ScheduledExecutorService executorService) {
    return new Builder(executorService);
  }

  public static class Builder {

    private final ScheduledExecutorService executorService;
    private final List<OnNextListener<CandleWrapper>> onCandleListeners = new ArrayList<>();
    private final List<OnNextListener<LastPriceWrapper>> onLastPriceListeners = new ArrayList<>();
    private final List<OnNextListener<OrderBookWrapper>> onOrderBookListeners = new ArrayList<>();
    private final List<OnNextListener<TradeWrapper>> onTradeListeners = new ArrayList<>();
    private final List<OnNextListener<TradingStatusWrapper>> onTradingStatusListeners = new ArrayList<>();
    private final List<Runnable> onConnectListeners = new ArrayList<>();

    protected Builder(ScheduledExecutorService executorService) {
      this.executorService = executorService;
    }

    /**
     * Метод добавления листенера для обработки {@link CandleWrapper}
     *
     * @param onCandleListener Листенер для обработки {@link CandleWrapper}
     *                         <p>Можно задать в виде лямбы: <pre>{@code
     *                                                 candle -> log.info("{}", candle)
     *                                                 }</pre>
     * @return Билдер конфигурации обёртки
     */
    public Builder addOnCandleListener(OnNextListener<CandleWrapper> onCandleListener) {
      onCandleListeners.add(onCandleListener);
      return this;
    }

    /**
     * Метод добавления листенера для обработки {@link LastPriceWrapper}
     *
     * @param onLastPriceListener Листенер для обработки {@link LastPriceWrapper}
     *                            <p>Можно задать в виде лямбы: <pre>{@code
     *                                                       lastPrice -> log.info("{}", lastPrice)
     *                                                       }</pre>
     * @return Билдер конфигурации обёртки
     */
    public Builder addOnLastPriceListener(OnNextListener<LastPriceWrapper> onLastPriceListener) {
      onLastPriceListeners.add(onLastPriceListener);
      return this;
    }

    /**
     * Метод добавления листенера для обработки {@link OrderBookWrapper}
     *
     * @param onOrderBookListener Листенер для обработки {@link OrderBookWrapper}
     *                            <p>Можно задать в виде лямбы: <pre>{@code
     *                                                       orderBook -> log.info("{}", orderBook)
     *                                                       }</pre>
     * @return Билдер конфигурации обёртки
     */
    public Builder addOnOrderBookListener(OnNextListener<OrderBookWrapper> onOrderBookListener) {
      onOrderBookListeners.add(onOrderBookListener);
      return this;
    }

    /**
     * Метод добавления листенера для обработки {@link TradeWrapper}
     *
     * @param onTradeListener Листенер для обработки {@link TradingStatusWrapper}
     *                        <p>Можно задать в виде лямбы: <pre>{@code
     *                                               trade -> log.info("{}", trade)
     *                                               }</pre>
     * @return Билдер конфигурации обёртки
     */
    public Builder addOnTradeListener(OnNextListener<TradeWrapper> onTradeListener) {
      onTradeListeners.add(onTradeListener);
      return this;
    }

    /**
     * Метод добавления листенера для обработки {@link TradingStatusWrapper}
     *
     * @param onTradingStatusListener Листенер для обработки {@link TradingStatusWrapper}
     *                                <p>Можно задать в виде лямбы: <pre>{@code
     *                                                               tradingStatus -> log.info("{}", tradingStatus)
     *                                                               }</pre>
     * @return Билдер конфигурации обёртки
     */
    public Builder addOnTradingStatusListener(OnNextListener<TradingStatusWrapper> onTradingStatusListener) {
      onTradingStatusListeners.add(onTradingStatusListener);
      return this;
    }

    /**
     * Листенер для обработки успешной подписки на обновления. Срабатывает также при переподключении стрима
     *
     * @param onConnectListener Функция для обработки события успешной подписки
     *                          <p>Можно задать в виде лямбды:<pre>{@code
     *                                                   () -> logger.info("Stream connected!")
     *                                                   }</pre>
     * @return Билдер конфигурации обёртки над стримом
     */
    public Builder addOnConnectListener(Runnable onConnectListener) {
      onConnectListeners.add(onConnectListener);
      return this;
    }

    public MarketDataStreamWrapperConfiguration build() {
      return new MarketDataStreamWrapperConfiguration(
        executorService, onCandleListeners, onLastPriceListeners, onOrderBookListeners, onTradeListeners,
        onTradingStatusListeners, onConnectListeners
      );
    }
  }
}
