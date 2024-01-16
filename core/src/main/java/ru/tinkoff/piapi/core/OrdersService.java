package ru.tinkoff.piapi.core;

import ru.tinkoff.piapi.core.utils.DateUtils;
import ru.tinkoff.piapi.core.utils.Helpers;
import ru.tinkoff.piapi.core.utils.ValidationUtils;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.contract.v1.OrdersServiceGrpc.OrdersServiceBlockingStub;
import ru.tinkoff.piapi.contract.v1.OrdersServiceGrpc.OrdersServiceStub;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class OrdersService {
  private final OrdersServiceBlockingStub ordersBlockingStub;
  private final OrdersServiceStub ordersStub;
  private final boolean readonlyMode;

  OrdersService(@Nonnull OrdersServiceBlockingStub ordersBlockingStub,
                @Nonnull OrdersServiceStub ordersStub,
                boolean readonlyMode) {
    this.ordersBlockingStub = ordersBlockingStub;
    this.ordersStub = ordersStub;
    this.readonlyMode = readonlyMode;
  }


  /**
   * Разместить заявку.
   *
   * @param instrumentId figi / instrument_uid инструмента
   * @param quantity     количество лотов
   * @param price        цена (для лимитной заявки)
   * @param direction    покупка/продажа
   * @param accountId    id аккаунта
   * @param type         рыночная / лимитная заявка
   * @param orderId      уникальный идентификатор заявки
   * @return             Информация о выставлении поручения
   */
  @Nonnull
  public PostOrderResponse postOrderSync(@Nonnull String instrumentId,
                                         long quantity,
                                         @Nonnull Quotation price,
                                         @Nonnull OrderDirection direction,
                                         @Nonnull String accountId,
                                         @Nonnull OrderType type,
                                         @Nullable String orderId) {
    ValidationUtils.checkReadonly(readonlyMode);
    var finalOrderId = orderId == null ? UUID.randomUUID().toString() : orderId;

    return Helpers.unaryCall(() -> ordersBlockingStub.postOrder(
      PostOrderRequest.newBuilder()
        .setInstrumentId(instrumentId)
        .setQuantity(quantity)
        .setPrice(price)
        .setDirection(direction)
        .setAccountId(accountId)
        .setOrderType(type)
        .setOrderId(Helpers.preprocessInputOrderId(finalOrderId))
        .build()));
  }

  /**
   * @param instrumentId    figi / instrument_uid инструмента
   * @param quantity        количество лотов
   * @param price           цена (для лимитной заявки)
   * @param direction       покупка/продажа
   * @param accountId       id аккаунта
   * @param timeInForceType алгоритм исполнения поручения
   * @param orderId         уникальный идентификатор заявки
   * @return                Информация о выставлении поручения
   */
  @Nonnull
  public PostOrderResponse postLimitOrderSync(@Nonnull String instrumentId,
                                              long quantity,
                                              @Nonnull Quotation price,
                                              @Nonnull OrderDirection direction,
                                              @Nonnull String accountId,
                                              @Nonnull TimeInForceType timeInForceType,
                                              @Nullable String orderId) {
    return postLimitOrderSync(instrumentId, quantity, price, direction, accountId, timeInForceType, null, orderId);
  }

  /**
   * @param instrumentId    figi / instrument_uid инструмента
   * @param quantity        количество лотов
   * @param price           цена (для лимитной заявки)
   * @param direction       покупка/продажа
   * @param accountId       id аккаунта
   * @param timeInForceType алгоритм исполнения поручения
   * @param priceType       тип цены валюта/пункты, можно передавать null
   * @param orderId         уникальный идентификатор заявки
   * @return                Информация о выставлении поручения
   */
  @Nonnull
  public PostOrderResponse postLimitOrderSync(@Nonnull String instrumentId,
                                         long quantity,
                                         @Nonnull Quotation price,
                                         @Nonnull OrderDirection direction,
                                         @Nonnull String accountId,
                                         @Nonnull TimeInForceType timeInForceType,
                                         @Nullable PriceType priceType,
                                         @Nullable String orderId) {
    ValidationUtils.checkReadonly(readonlyMode);
    var finalOrderId = orderId == null ? UUID.randomUUID().toString() : orderId;

    return Helpers.unaryCall(() -> ordersBlockingStub.postOrder(
      PostOrderRequest.newBuilder()
        .setInstrumentId(instrumentId)
        .setQuantity(quantity)
        .setPrice(price)
        .setDirection(direction)
        .setAccountId(accountId)
        .setOrderType(OrderType.ORDER_TYPE_LIMIT)
        .setTimeInForce(timeInForceType)
        .setPriceType(priceType == null ? PriceType.PRICE_TYPE_UNSPECIFIED : priceType)
        .setOrderId(Helpers.preprocessInputOrderId(finalOrderId))
        .build()));
  }

  @Nonnull
  public Instant cancelOrderSync(@Nonnull String accountId,
                                 @Nonnull String orderId) {
    ValidationUtils.checkReadonly(readonlyMode);

    var responseTime = Helpers.unaryCall(() -> ordersBlockingStub.cancelOrder(
        CancelOrderRequest.newBuilder()
          .setAccountId(accountId)
          .setOrderId(orderId)
          .build())
      .getTime());

    return DateUtils.timestampToInstant(responseTime);
  }

  @Nonnull
  public OrderState getOrderStateSync(@Nonnull String accountId,
                                      @Nonnull String orderId) {
    return Helpers.unaryCall(() -> ordersBlockingStub.getOrderState(
      GetOrderStateRequest.newBuilder()
        .setAccountId(accountId)
        .setOrderId(orderId)
        .build()));
  }

  @Nonnull
  public OrderState getOrderStateSync(@Nonnull String accountId,
                                      @Nonnull String orderId,
                                      @Nonnull PriceType priceType) {
    return Helpers.unaryCall(() -> ordersBlockingStub.getOrderState(
      GetOrderStateRequest.newBuilder()
        .setAccountId(accountId)
        .setOrderId(orderId)
        .setPriceType(priceType)
        .build()));
  }

  @Nonnull
  public List<OrderState> getOrdersSync(@Nonnull String accountId) {
    return Helpers.unaryCall(() -> ordersBlockingStub.getOrders(
        GetOrdersRequest.newBuilder()
          .setAccountId(accountId)
          .build())
      .getOrdersList());
  }

  /**
   * Разместить заявку асинхронно.
   *
   * @param instrumentId figi / instrument_uid инструмента
   * @param quantity     количество лотов
   * @param price        цена (для лимитной заявки)
   * @param direction    покупка/продажа
   * @param accountId    id аккаунта
   * @param type         рыночная / лимитная заявка
   * @param orderId      уникальный идентификатор заявки
   * @return             Информация о выставлении поручения
   */
  @Nonnull
  public CompletableFuture<PostOrderResponse> postOrder(@Nonnull String instrumentId,
                                                        long quantity,
                                                        @Nonnull Quotation price,
                                                        @Nonnull OrderDirection direction,
                                                        @Nonnull String accountId,
                                                        @Nonnull OrderType type,
                                                        @Nullable String orderId) {
    ValidationUtils.checkReadonly(readonlyMode);
    var finalOrderId = orderId == null ? UUID.randomUUID().toString() : orderId;

    return Helpers.unaryAsyncCall(
      observer -> ordersStub.postOrder(
        PostOrderRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .setQuantity(quantity)
          .setPrice(price)
          .setDirection(direction)
          .setAccountId(accountId)
          .setOrderType(type)
          .setOrderId(Helpers.preprocessInputOrderId(finalOrderId))
          .build(),
        observer));
  }

  /**
   * @param instrumentId    figi / instrument_uid инструмента
   * @param quantity        количество лотов
   * @param price           цена (для лимитной заявки)
   * @param direction       покупка/продажа
   * @param accountId       id аккаунта
   * @param timeInForceType алгоритм исполнения поручения
   * @param orderId         уникальный идентификатор заявки
   * @return                Информация о выставлении поручения
   */
  @Nonnull
  public CompletableFuture<PostOrderResponse> postLimitOrder(@Nonnull String instrumentId,
                                                             long quantity,
                                                             @Nonnull Quotation price,
                                                             @Nonnull OrderDirection direction,
                                                             @Nonnull String accountId,
                                                             @Nonnull TimeInForceType timeInForceType,
                                                             @Nullable String orderId) {
    return postLimitOrder(instrumentId, quantity, price, direction, accountId, timeInForceType, null, orderId);
  }

  /**
   * @param instrumentId    figi / instrument_uid инструмента
   * @param quantity        количество лотов
   * @param price           цена (для лимитной заявки)
   * @param direction       покупка/продажа
   * @param accountId       id аккаунта
   * @param timeInForceType алгоритм исполнения поручения
   * @param priceType       тип цены валюта/пункты, можно передавать null
   * @param orderId         уникальный идентификатор заявки
   * @return                Информация о выставлении поручения
   */
  @Nonnull
  public CompletableFuture<PostOrderResponse> postLimitOrder(@Nonnull String instrumentId,
                                                        long quantity,
                                                        @Nonnull Quotation price,
                                                        @Nonnull OrderDirection direction,
                                                        @Nonnull String accountId,
                                                        @Nonnull TimeInForceType timeInForceType,
                                                        @Nullable PriceType priceType,
                                                        @Nullable String orderId) {
    ValidationUtils.checkReadonly(readonlyMode);
    var finalOrderId = orderId == null ? UUID.randomUUID().toString() : orderId;

    return Helpers.unaryAsyncCall(
      observer -> ordersStub.postOrder(
        PostOrderRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .setQuantity(quantity)
          .setPrice(price)
          .setDirection(direction)
          .setAccountId(accountId)
          .setOrderType(OrderType.ORDER_TYPE_LIMIT)
          .setTimeInForce(timeInForceType)
          .setPriceType(priceType == null ? PriceType.PRICE_TYPE_UNSPECIFIED : priceType)
          .setOrderId(Helpers.preprocessInputOrderId(finalOrderId))
          .build(),
        observer));
  }

  @Nonnull
  public CompletableFuture<Instant> cancelOrder(@Nonnull String accountId,
                                                @Nonnull String orderId) {
    ValidationUtils.checkReadonly(readonlyMode);

    return Helpers.<CancelOrderResponse>unaryAsyncCall(
        observer -> ordersStub.cancelOrder(
          CancelOrderRequest.newBuilder()
            .setAccountId(accountId)
            .setOrderId(orderId)
            .build(),
          observer))
      .thenApply(response -> DateUtils.timestampToInstant(response.getTime()));
  }

  @Nonnull
  public CompletableFuture<OrderState> getOrderState(@Nonnull String accountId,
                                                     @Nonnull String orderId) {
    return Helpers.unaryAsyncCall(
      observer -> ordersStub.getOrderState(
        GetOrderStateRequest.newBuilder()
          .setAccountId(accountId)
          .setOrderId(orderId)
          .build(),
        observer));
  }

  @Nonnull
  public CompletableFuture<OrderState> getOrderState(@Nonnull String accountId,
                                      @Nonnull String orderId,
                                      @Nonnull PriceType priceType) {
    return Helpers.unaryAsyncCall(
      observer -> ordersStub.getOrderState(
        GetOrderStateRequest.newBuilder()
          .setAccountId(accountId)
          .setOrderId(orderId)
          .setPriceType(priceType)
          .build(),
        observer));
  }

  @Nonnull
  public CompletableFuture<List<OrderState>> getOrders(@Nonnull String accountId) {
    return Helpers.<GetOrdersResponse>unaryAsyncCall(
        observer -> ordersStub.getOrders(
          GetOrdersRequest.newBuilder()
            .setAccountId(accountId)
            .build(),
          observer))
      .thenApply(GetOrdersResponse::getOrdersList);
  }

  /**
   * Последовательное выполнение 2 операций - отмены и выставления нового ордера.
   *
   * @param accountId      Номер счета
   * @param quantity       Количество лотов
   * @param price          Цена за 1 инструмент
   * @param idempotencyKey Новый идентификатор запроса выставления поручения для целей идемпотентности. Максимальная длина 36 символов. Перезатирает старый ключ
   * @param orderId        Идентификатор заявки на бирже
   * @param priceType      Тип цены валюта/пункты, можно передавать null
   * @return Информация о выставлении поручения
   */
  @Nonnull
  public CompletableFuture<PostOrderResponse> replaceOrder(@Nonnull String accountId,
                                                           long quantity,
                                                           @Nonnull Quotation price,
                                                           @Nullable String idempotencyKey,
                                                           @Nonnull String orderId,
                                                           @Nullable PriceType priceType) {
    var request = ReplaceOrderRequest.newBuilder()
      .setAccountId(accountId)
      .setPrice(price)
      .setQuantity(quantity)
      .setIdempotencyKey(idempotencyKey == null ? "" : idempotencyKey)
      .setOrderId(orderId)
      .setPriceType(priceType == null ? PriceType.PRICE_TYPE_UNSPECIFIED : priceType)
      .build();
    return Helpers.unaryAsyncCall(
      observer -> ordersStub.replaceOrder(request, observer));
  }

  /**
   * Последовательное выполнение 2 операций - отмены и выставления нового ордера.
   *
   * @param accountId      Номер счета
   * @param quantity       Количество лотов
   * @param price          Цена за 1 инструмент
   * @param idempotencyKey Новый идентификатор запроса выставления поручения для целей идемпотентности. Максимальная длина 36 символов. Перезатирает старый ключ
   * @param orderId        Идентификатор заявки на бирже
   * @param priceType      Тип цены валюта/пункты, можно передавать null
   * @return Информация о выставлении поручения
   */
  @Nonnull
  public PostOrderResponse replaceOrderSync(@Nonnull String accountId,
                                            long quantity,
                                            @Nonnull Quotation price,
                                            @Nullable String idempotencyKey,
                                            @Nonnull String orderId,
                                            @Nullable PriceType priceType) {
    var request = ReplaceOrderRequest.newBuilder()
      .setAccountId(accountId)
      .setPrice(price)
      .setQuantity(quantity)
      .setIdempotencyKey(idempotencyKey == null ? "" : idempotencyKey)
      .setOrderId(orderId)
      .setPriceType(priceType == null ? PriceType.PRICE_TYPE_UNSPECIFIED : priceType)
      .build();
    return Helpers.unaryCall(() -> ordersBlockingStub.replaceOrder(request));
  }

  /**
   * Метод получения информации о коммисиях при выставлении торгового поручения
   * @param accountId     Номер счета
   * @param instrumentId  figi / instrument_uid инструмента
   * @param quantity      Количество лотов
   * @param price         Цена за 1 инструмент
   * @param direction     покупка/продажа
   * @return Информация о максимальной цене заявки с учетом комиссии
   */
  @Nonnull
  public CompletableFuture<GetOrderPriceResponse> getOrderPrice(@Nonnull String accountId,
                                                                @Nonnull String instrumentId,
                                                                long quantity,
                                                                @Nonnull Quotation price,
                                                                @Nonnull OrderDirection direction) {
    return Helpers.unaryAsyncCall(
      observer -> ordersStub.getOrderPrice(
        GetOrderPriceRequest.newBuilder()
          .setAccountId(accountId)
          .setPrice(price)
          .setQuantity(quantity)
          .setDirection(direction)
          .setInstrumentId(instrumentId)
          .build(),
        observer
      ));
  }

  /**
   * Метод получения информации о коммисиях при выставлении торгового поручения
   * @param accountId     Номер счета
   * @param instrumentId  figi / instrument_uid инструмента
   * @param quantity      Количество лотов
   * @param price         Цена за 1 инструмент
   * @param direction     покупка/продажа
   * @return Информация о максимальной цене заявки с учетом комиссии
   */
  @Nonnull
  public GetOrderPriceResponse getOrderPriceSync(@Nonnull String accountId,
                                                 @Nonnull String instrumentId,
                                                 long quantity,
                                                 @Nonnull Quotation price,
                                                 @Nonnull OrderDirection direction) {
    return Helpers.unaryCall(
      () -> ordersBlockingStub.getOrderPrice(
        GetOrderPriceRequest.newBuilder()
          .setAccountId(accountId)
          .setPrice(price)
          .setQuantity(quantity)
          .setDirection(direction)
          .setInstrumentId(instrumentId)
          .build())
    );
  }
}
