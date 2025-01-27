package ru.ttech.piapi.core.connector.streaming;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class StreamResponseObserver<RespT> implements StreamObserver<RespT> {

  private static final Logger logger = LoggerFactory.getLogger(StreamResponseObserver.class);
  private final AtomicLong lastResponseTime = new AtomicLong(0);
  private final List<OnNextListener<RespT>> onNextListeners;
  private final List<OnErrorListener> onErrorListeners;
  private final List<OnCompleteListener> onCompleteListeners;
  private boolean healthcheckEnabled = false;

  StreamResponseObserver(
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
    if (healthcheckEnabled) {
      lastResponseTime.set(System.currentTimeMillis());
    }
    onNextListeners.forEach(listener -> {
      try {
        listener.onNext(response);
      } catch (Throwable e) {
        logger.error("Произошла ошибка при обработке ответа: {}", e.getMessage());
      }
    });
  }

  @Override
  public void onError(Throwable throwable) {
    onErrorListeners.forEach(listener -> {
      try {
        listener.onError(throwable);
      } catch (Throwable e) {
        logger.error("Произошла ошибка при обработке ошибки: {}", e.getMessage());
      }
    });
  }

  @Override
  public void onCompleted() {
    onCompleteListeners.forEach(onCompleteListener -> {
      try {
        onCompleteListener.onComplete();
      } catch (Throwable e) {
        logger.error("Произошла ошибка при завершении стрима: {}", e.getMessage());
      }
    });
  }

  public long getLastResponseTime() {
    return lastResponseTime.get();
  }

  public void resetLastResponseTime() {
    lastResponseTime.set(0);
  }

  public void enableHealthcheck(boolean healthcheckEnabled) {
    this.healthcheckEnabled = healthcheckEnabled;
  }
}
