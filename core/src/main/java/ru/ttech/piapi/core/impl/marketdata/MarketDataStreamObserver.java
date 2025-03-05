package ru.ttech.piapi.core.impl.marketdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.ttech.piapi.core.connector.streaming.StreamResponseObserver;
import ru.ttech.piapi.core.connector.streaming.listeners.OnCompleteListener;
import ru.ttech.piapi.core.connector.streaming.listeners.OnErrorListener;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MarketDataStreamObserver extends StreamResponseObserver<MarketDataResponse> {

  private static final Logger logger = LoggerFactory.getLogger(MarketDataStreamObserver.class);
  private final Map<MarketDataResponseType, List<OnNextListener<MarketDataResponse>>> onResponseListeners;

  public MarketDataStreamObserver(
    Map<MarketDataResponseType, List<OnNextListener<MarketDataResponse>>> onResponseListeners,
    List<OnNextListener<MarketDataResponse>> onNextListeners,
    List<OnErrorListener> onErrorListeners,
    List<OnCompleteListener> onCompleteListeners
  ) {
    super(onNextListeners, onErrorListeners, onCompleteListeners);
    this.onResponseListeners = onResponseListeners;
  }

  @Override
  public void onNext(MarketDataResponse response) {
    var responseType = getResponseType(response);
    super.onNext(response);
    Optional.ofNullable(onResponseListeners.get(responseType))
      .ifPresent(listeners -> listeners.forEach(listener -> {
        try {
          listener.onNext(response);
        } catch (Throwable e) {
          logger.error("Произошла ошибка при обработке ответа: {}", e.getMessage());
        }
      }));
  }

  private MarketDataResponseType getResponseType(MarketDataResponse marketDataResponse) {
    if (marketDataResponse.hasCandle()) {
      return MarketDataResponseType.CANDLE;
    } else if (marketDataResponse.hasLastPrice()) {
      return MarketDataResponseType.LAST_PRICE;
    } else if (marketDataResponse.hasOrderbook()) {
      return MarketDataResponseType.ORDER_BOOK;
    } else if (marketDataResponse.hasTrade()) {
      return MarketDataResponseType.TRADE;
    } else if (marketDataResponse.hasTradingStatus()) {
      return MarketDataResponseType.TRADING_STATUS;
    }
    return MarketDataResponseType.OTHER;
  }
}
