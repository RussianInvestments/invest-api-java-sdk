package ru.ttech.piapi.core.impl.marketdata.subscription;

import ru.tinkoff.piapi.contract.v1.SubscribeCandlesResponse;
import ru.tinkoff.piapi.contract.v1.SubscribeInfoResponse;
import ru.tinkoff.piapi.contract.v1.SubscribeLastPriceResponse;
import ru.tinkoff.piapi.contract.v1.SubscribeOrderBookResponse;
import ru.tinkoff.piapi.contract.v1.SubscribeTradesResponse;
import ru.tinkoff.piapi.contract.v1.SubscriptionAction;
import ru.ttech.piapi.core.impl.marketdata.MarketDataResponseType;

import java.util.stream.Collectors;

public class SubscriptionResultMapper {

  public static MarketDataSubscriptionResult map(SubscribeCandlesResponse subscribeCandlesResponse) {
    return new MarketDataSubscriptionResult(
      mapCandlesSubscriptionAction(subscribeCandlesResponse),
      MarketDataResponseType.CANDLE,
      subscribeCandlesResponse.getCandlesSubscriptionsList().stream()
        .collect(Collectors.toMap(
          candleSubscription -> new Instrument(candleSubscription.getInstrumentUid(), candleSubscription.getInterval()),
          candleSubscription -> mapSubscriptionStatus(candleSubscription.getSubscriptionStatus()))
        )
    );
  }

  public static MarketDataSubscriptionResult map(SubscribeLastPriceResponse subscribeLastPriceResponse) {
    return new MarketDataSubscriptionResult(
      mapLastPriceSubscriptionAction(subscribeLastPriceResponse),
      MarketDataResponseType.LAST_PRICE,
      subscribeLastPriceResponse.getLastPriceSubscriptionsList().stream()
        .collect(Collectors.toMap(
          subscription -> new Instrument(subscription.getInstrumentUid()),
          subscription -> mapSubscriptionStatus(subscription.getSubscriptionStatus()))
        )
    );
  }

  public static MarketDataSubscriptionResult map(SubscribeOrderBookResponse subscribeOrderBookResponse) {
    return new MarketDataSubscriptionResult(
      mapOrderBookSubscriptionAction(subscribeOrderBookResponse),
      MarketDataResponseType.ORDER_BOOK,
      subscribeOrderBookResponse.getOrderBookSubscriptionsList().stream()
        .collect(Collectors.toMap(
          subscription -> new Instrument(
            subscription.getInstrumentUid(), subscription.getDepth(), subscription.getOrderBookType()
          ),
          subscription -> mapSubscriptionStatus(subscription.getSubscriptionStatus()))
        )
    );
  }

  public static MarketDataSubscriptionResult map(SubscribeTradesResponse subscribeTradesResponse) {
    return new MarketDataSubscriptionResult(
      mapTradesSubscriptionAction(subscribeTradesResponse),
      MarketDataResponseType.TRADE,
      subscribeTradesResponse.getTradeSubscriptionsList().stream()
        .collect(Collectors.toMap(
          subscription -> new Instrument(subscription.getInstrumentUid()),
          subscription -> mapSubscriptionStatus(subscription.getSubscriptionStatus()))
        )
    );
  }

  public static MarketDataSubscriptionResult map(SubscribeInfoResponse subscribeInfoResponse) {
    return new MarketDataSubscriptionResult(
      mapInfoSubscriptionAction(subscribeInfoResponse),
      MarketDataResponseType.TRADING_STATUS,
      subscribeInfoResponse.getInfoSubscriptionsList().stream()
        .collect(Collectors.toMap(
          subscription -> new Instrument(subscription.getInstrumentUid()),
          subscription -> mapSubscriptionStatus(subscription.getSubscriptionStatus()))
        )
    );
  }

  private static SubscriptionStatus mapSubscriptionStatus(
    ru.tinkoff.piapi.contract.v1.SubscriptionStatus subscriptionStatus
  ) {
    return subscriptionStatus == ru.tinkoff.piapi.contract.v1.SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS
      ? SubscriptionStatus.OK
      : SubscriptionStatus.ERROR;
  }

  private static RequestAction mapCandlesSubscriptionAction(SubscribeCandlesResponse response) {
    if (response.getCandlesSubscriptionsList().isEmpty()) {
      return RequestAction.UNKNOWN;
    }
    return mapSubscriptionAction(response.getCandlesSubscriptionsList().get(0).getSubscriptionAction());
  }

  private static RequestAction mapLastPriceSubscriptionAction(SubscribeLastPriceResponse response) {
    if (response.getLastPriceSubscriptionsList().isEmpty()) {
      return RequestAction.UNKNOWN;
    }
    return mapSubscriptionAction(response.getLastPriceSubscriptionsList().get(0).getSubscriptionAction());
  }

  private static RequestAction mapOrderBookSubscriptionAction(SubscribeOrderBookResponse response) {
    if (response.getOrderBookSubscriptionsList().isEmpty()) {
      return RequestAction.UNKNOWN;
    }
    return mapSubscriptionAction(response.getOrderBookSubscriptionsList().get(0).getSubscriptionAction());
  }

  private static RequestAction mapTradesSubscriptionAction(SubscribeTradesResponse response) {
    if (response.getTradeSubscriptionsList().isEmpty()) {
      return RequestAction.UNKNOWN;
    }
    return mapSubscriptionAction(response.getTradeSubscriptionsList().get(0).getSubscriptionAction());
  }

  private static RequestAction mapInfoSubscriptionAction(SubscribeInfoResponse response) {
    if (response.getInfoSubscriptionsList().isEmpty()) {
      return RequestAction.UNKNOWN;
    }
    return mapSubscriptionAction(response.getInfoSubscriptionsList().get(0).getSubscriptionAction());
  }

  private static RequestAction mapSubscriptionAction(SubscriptionAction action) {
    if (action == SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE) {
      return RequestAction.SUBSCRIBE;
    } else if (action == SubscriptionAction.SUBSCRIPTION_ACTION_UNSUBSCRIBE) {
      return RequestAction.UNSUBSCRIBE;
    }
    return RequestAction.UNKNOWN;
  }
}
