package ru.ttech.piapi.core.impl.marketdata.util;

import ru.tinkoff.piapi.contract.v1.MarketDataRequest;
import ru.ttech.piapi.core.impl.marketdata.MarketDataResponseType;
import ru.ttech.piapi.core.impl.marketdata.subscription.Instrument;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MarketDataRequestUtil {

  public static MarketDataResponseType determineRequestType(MarketDataRequest request) {
    if (request.hasSubscribeCandlesRequest()) {
      return MarketDataResponseType.CANDLE;
    } else if (request.hasSubscribeLastPriceRequest()) {
      return MarketDataResponseType.LAST_PRICE;
    } else if (request.hasSubscribeOrderBookRequest()) {
      return MarketDataResponseType.ORDER_BOOK;
    } else if (request.hasSubscribeTradesRequest()) {
      return MarketDataResponseType.TRADE;
    } else if (request.hasSubscribeInfoRequest()) {
      return MarketDataResponseType.TRADING_STATUS;
    }
    return MarketDataResponseType.OTHER;
  }

  public static List<Instrument> extractInstruments(MarketDataRequest request) {
    if (request.hasSubscribeCandlesRequest()) {
      return request.getSubscribeCandlesRequest().getInstrumentsList().stream()
        .map(instrument -> new Instrument(instrument.getInstrumentId(), instrument.getInterval()))
        .collect(Collectors.toList());
    } else if (request.hasSubscribeLastPriceRequest()) {
      return request.getSubscribeLastPriceRequest().getInstrumentsList().stream()
        .map(instrument -> new Instrument(instrument.getInstrumentId()))
        .collect(Collectors.toList());
    } else if (request.hasSubscribeOrderBookRequest()) {
      return request.getSubscribeOrderBookRequest().getInstrumentsList().stream()
        .map(instrument -> new Instrument(instrument.getInstrumentId(), instrument.getDepth(), instrument.getOrderBookType()))
        .collect(Collectors.toList());
    } else if (request.hasSubscribeTradesRequest()) {
      return request.getSubscribeTradesRequest().getInstrumentsList().stream()
        .map(instrument -> new Instrument(instrument.getInstrumentId()))
        .collect(Collectors.toList());
    } else if (request.hasSubscribeInfoRequest()) {
      return request.getSubscribeInfoRequest().getInstrumentsList().stream()
        .map(instrument -> new Instrument(instrument.getInstrumentId()))
        .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }
}
