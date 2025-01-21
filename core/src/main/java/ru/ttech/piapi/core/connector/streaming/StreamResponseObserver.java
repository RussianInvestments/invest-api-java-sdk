package ru.ttech.piapi.core.connector.streaming;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class StreamResponseObserver<RespT> implements StreamObserver<RespT> {

  private static final Logger logger = LoggerFactory.getLogger(StreamResponseObserver.class);
  private final List<OnNextListener<RespT>> onNextListeners;
  private final List<OnErrorListener> onErrorListeners;
  private final List<OnCompleteListener> onCompleteListeners;

  public StreamResponseObserver(
    List<OnNextListener<RespT>> onNextListeners,
    List<OnErrorListener> onErrorListeners,
    List<OnCompleteListener> onCompleteListeners
  ) {
    this.onNextListeners = onNextListeners;
    this.onErrorListeners = onErrorListeners;
    this.onCompleteListeners = onCompleteListeners;
  }

  @Override
  public void onNext(RespT response) {
    try {
      onNextListeners.forEach(listener -> listener.onNext(response));
    } catch (Throwable e) {
      logger.error("Произошла ошибка при обработке ответа: {}", e.getMessage());
    }
  }

  @Override
  public void onError(Throwable throwable) {
    try {
      onErrorListeners.forEach(listener -> listener.onError(throwable));
    } catch (Throwable e) {
      logger.error("Произошла ошибка при обработке ошибки: {}", e.getMessage());
    }
  }

  @Override
  public void onCompleted() {
    try {
      onCompleteListeners.forEach(OnCompleteListener::onComplete);
    } catch (Throwable e) {
      logger.error("Произошла ошибка при завершении стрима: {}", e.getMessage());
    }
  }
}
