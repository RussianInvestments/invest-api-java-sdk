package ru.ttech.piapi.core.impl.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamResponse;
import ru.ttech.piapi.core.connector.streaming.StreamResponseObserver;
import ru.ttech.piapi.core.connector.streaming.listeners.OnCompleteListener;
import ru.ttech.piapi.core.connector.streaming.listeners.OnErrorListener;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class OrderStateStreamObserver extends StreamResponseObserver<OrderStateStreamResponse> {

  private static final Logger logger = LoggerFactory.getLogger(OrderStateStreamObserver.class);
  private final List<OnNextListener<OrderStateStreamResponse>> onResponseListeners;
  private final AtomicLong lastPingTime = new AtomicLong(0);

  protected OrderStateStreamObserver(
    List<OnNextListener<OrderStateStreamResponse>> onResponseListeners,
    List<OnNextListener<OrderStateStreamResponse>> onNextListeners,
    List<OnErrorListener> onErrorListeners,
    List<OnCompleteListener> onCompleteListeners
  ) {
    super(onNextListeners, onErrorListeners, onCompleteListeners);
    this.onResponseListeners = onResponseListeners;
  }

  @Override
  public void onNext(OrderStateStreamResponse response) {
    var responseType = getResponseType(response);
    if (responseType == OrderStateResponseType.OTHER) {
      super.onNext(response);
    } else {
      onResponseListeners.forEach(listener -> {
          try {
            listener.onNext(response);
          } catch (Throwable e) {
            logger.error("Произошла ошибка при обработке ответа: {}", e.getMessage());
          }
        }
      );
    }
  }

  private OrderStateResponseType getResponseType(OrderStateStreamResponse orderStateStreamResponse) {
    if (orderStateStreamResponse.hasOrderState()) {
      return OrderStateResponseType.ORDER_STATE;
    } else if (orderStateStreamResponse.hasSubscription()) {
      return OrderStateResponseType.SUBSCRIPTION_RESULT;
    } else if (orderStateStreamResponse.hasPing()) {
      return OrderStateResponseType.PING;
    }
    return OrderStateResponseType.OTHER;
  }
}
