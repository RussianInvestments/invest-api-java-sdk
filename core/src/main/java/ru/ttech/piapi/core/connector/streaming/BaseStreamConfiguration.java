package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;
import ru.ttech.piapi.core.connector.streaming.listeners.OnCompleteListener;
import ru.ttech.piapi.core.connector.streaming.listeners.OnErrorListener;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

abstract class BaseStreamConfiguration<S extends AbstractAsyncStub<S>, ReqT, RespT> {
  protected final Function<Channel, S> stubConstructor;
  protected final MethodDescriptor<ReqT, RespT> method;
  protected final List<OnNextListener<RespT>> onNextListeners;
  protected final List<OnErrorListener> onErrorListeners;
  protected final List<OnCompleteListener> onCompleteListeners;

  protected BaseStreamConfiguration(
    Function<Channel, S> stubConstructor,
    MethodDescriptor<ReqT, RespT> method,
    List<OnNextListener<RespT>> onNextListeners,
    List<OnErrorListener> onErrorListeners,
    List<OnCompleteListener> onCompleteListeners
  ) {
    this.stubConstructor = stubConstructor;
    this.method = method;
    this.onNextListeners = onNextListeners;
    this.onErrorListeners = onErrorListeners;
    this.onCompleteListeners = onCompleteListeners;
  }

  protected Function<Channel, S> getStubConstructor() {
    return stubConstructor;
  }

  protected Supplier<StreamObserver<RespT>> getResponseObserverCreator() {
    return () -> new StreamResponseObserver<>(onNextListeners, onErrorListeners, onCompleteListeners);
  }

  protected abstract static class BaseBuilder<
    S extends AbstractAsyncStub<S>,
    ReqT,
    RespT,
    B extends BaseBuilder<S, ReqT, RespT, B>
    > {
    protected final Function<Channel, S> stubConstructor;
    protected final MethodDescriptor<ReqT, RespT> method;
    protected final List<OnNextListener<RespT>> onNextListeners = new LinkedList<>();
    protected final List<OnErrorListener> onErrorListeners = new LinkedList<>();
    protected final List<OnCompleteListener> onCompleteListeners = new LinkedList<>();

    protected BaseBuilder(
      Function<Channel, S> stubConstructor,
      MethodDescriptor<ReqT, RespT> method
    ) {
      this.stubConstructor = stubConstructor;
      this.method = method;
    }

    /**
     * Метод добавления листенера нового сообщения в стриме
     *
     * @param onNextListener Листенер нового сообщения в стриме
     *                       <p>Можно задать в виде лямбы:<pre>{@code
     *                                                                   response -> log.info("Сообщение: {}", response)
     *                                                                   }</pre>
     * @return Билдер конфигурации обёртки над стримом
     */
    @SuppressWarnings("unchecked")
    public B addOnNextListener(OnNextListener<RespT> onNextListener) {
      onNextListeners.add(onNextListener);
      return (B) this;
    }

    /**
     * Метод добавления листенера ошибок в стриме
     *
     * @param onErrorListener Листенер ошибок в стриме
     *                        <p>Можно задать в виде лямбы:<pre>{@code
     *                                                                      throwable -> log.error("Ошибка: {}", throwable.getMessage())
     *                                                                      }</pre>
     * @return Билдер конфигурации обёртки над стримом
     */
    @SuppressWarnings("unchecked")
    public B addOnErrorListener(OnErrorListener onErrorListener) {
      onErrorListeners.add(onErrorListener);
      return (B) this;
    }

    /**
     * Метод добавления листенера успешного завершения стрима
     *
     * @param onCompleteListener Листенер успешного завершения стрима
     *                           <p>Можно задать в виде лямбы: <pre>{@code
     *                                                                               () -> log.info("Стрим завершен")
     *                                                                               }</pre>
     * @return Билдер конфигурации обёртки над стримом
     */
    @SuppressWarnings("unchecked")
    public B addOnCompleteListener(OnCompleteListener onCompleteListener) {
      onCompleteListeners.add(onCompleteListener);
      return (B) this;
    }
  }
}
