package ru.ttech.piapi.core.connector.resilience;

import ru.ttech.piapi.core.connector.streaming.ServerSideStreamConfiguration;
import ru.ttech.piapi.core.connector.streaming.ServerSideStreamWrapper;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Конфигурация resilience-обёртки {@link ResilienceServerSideStreamWrapper} над {@link ServerSideStreamWrapper}
 *
 * @param <ReqT>
 * @param <RespT>
 */
public abstract class ResilienceServerSideStreamWrapperConfiguration<ReqT, RespT> {

  protected final ScheduledExecutorService executorService;
  protected final List<OnNextListener<RespT>> onResponseListeners;
  protected final List<Runnable> onConnectListeners;

  protected ResilienceServerSideStreamWrapperConfiguration(
    ScheduledExecutorService executorService,
    List<OnNextListener<RespT>> onResponseListeners,
    List<Runnable> onConnectListeners
  ) {
    this.executorService =  executorService;
    this.onResponseListeners = onResponseListeners;
    this.onConnectListeners = onConnectListeners;
  }

  public ScheduledExecutorService getExecutorService() {
    return executorService;
  }

  public List<Runnable> getOnConnectListeners() {
    return onConnectListeners;
  }

  public abstract Function<ReqT, ServerSideStreamConfiguration.Builder<?, ReqT, RespT>> getConfigurationBuilder(int pingDelay);

  public abstract BiFunction<ReqT, RespT, Optional<ReqT>> getSubscriptionResultProcessor();

  public abstract static class Builder<ReqT, RespT> {

    protected final ScheduledExecutorService executorService;
    protected List<OnNextListener<RespT>> onResponseListeners = new ArrayList<>();
    protected List<Runnable> onConnectListeners = new ArrayList<>();

    protected Builder(ScheduledExecutorService executorService) {
      this.executorService = executorService;
    }

    /**
     * Листенер для обработки нового сообщения в стриме
     *
     * @param onResponseListener Функция для обработки успешной подписки
     *                           <p>Можно задать в виде лямбды:<pre>{@code
     *                           response -> logger.info("New update: {}", response)
     *                           }</pre></p>
     * @return Билдер конфигурации обёртки над стримом
     */
    public Builder<ReqT, RespT> addOnResponseListener(OnNextListener<RespT> onResponseListener) {
      onResponseListeners.add(onResponseListener);
      return this;
    }

    /**
     * Листенер для обработки успешной подписки на обновления. Срабатывает также при переподключении стрима
     *
     * @param onConnectListener Функция для обработки события успешной подписки
     *                          <p>Можно задать в виде лямбды:<pre>{@code
     *                          () -> logger.info("Stream connected!")
     *                          }</pre></p>
     * @return Билдер конфигурации обёртки над стримом
     */
    public Builder<ReqT, RespT> addOnConnectListener(Runnable onConnectListener) {
      onConnectListeners.add(onConnectListener);
      return this;
    }

    public abstract ResilienceServerSideStreamWrapperConfiguration<ReqT, RespT> build();
  }
}
