package ru.tinkoff.piapi.core;

import com.google.protobuf.Timestamp;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.utils.DateUtils;
import ru.tinkoff.piapi.core.utils.Helpers;
import ru.tinkoff.piapi.core.utils.ValidationUtils;
import ru.tinkoff.piapi.contract.v1.StopOrdersServiceGrpc.StopOrdersServiceBlockingStub;
import ru.tinkoff.piapi.contract.v1.StopOrdersServiceGrpc.StopOrdersServiceStub;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StopOrdersService {
  private final StopOrdersServiceBlockingStub stopOrdersBlockingStub;
  private final StopOrdersServiceStub stopOrdersStub;
  private final boolean readonlyMode;
  private final boolean sandboxMode;

  StopOrdersService(@Nonnull StopOrdersServiceBlockingStub stopOrdersBlockingStub,
                    @Nonnull StopOrdersServiceStub stopOrdersStub,
                    boolean readonlyMode,
                    boolean sandboxMode) {
    this.sandboxMode = sandboxMode;
    this.stopOrdersBlockingStub = stopOrdersBlockingStub;
    this.stopOrdersStub = stopOrdersStub;
    this.readonlyMode = readonlyMode;
  }

  @Nonnull
  public String postStopOrderGoodTillCancelSync(@Nonnull String instrumentId,
                                                long quantity,
                                                @Nonnull Quotation price,
                                                @Nonnull Quotation stopPrice,
                                                @Nonnull StopOrderDirection direction,
                                                @Nonnull String accountId,
                                                @Nonnull StopOrderType type) {
    ValidationUtils.checkReadonly(readonlyMode);
    ValidationUtils.checkSandbox(sandboxMode);

    return Helpers.unaryCall(() -> stopOrdersBlockingStub.postStopOrder(
        PostStopOrderRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .setQuantity(quantity)
          .setPrice(price)
          .setStopPrice(stopPrice)
          .setDirection(direction)
          .setAccountId(accountId)
          .setExpirationType(StopOrderExpirationType.STOP_ORDER_EXPIRATION_TYPE_GOOD_TILL_CANCEL)
          .setStopOrderType(type)
          .build())
      .getStopOrderId());
  }

  @Nonnull
  public String postStopOrderGoodTillDateSync(@Nonnull String instrumentId,
                                              long quantity,
                                              @Nonnull Quotation price,
                                              @Nonnull Quotation stopPrice,
                                              @Nonnull StopOrderDirection direction,
                                              @Nonnull String accountId,
                                              @Nonnull StopOrderType type,
                                              @Nonnull Instant expireDate) {
    ValidationUtils.checkReadonly(readonlyMode);
    ValidationUtils.checkSandbox(sandboxMode);

    return Helpers.unaryCall(() -> stopOrdersBlockingStub.postStopOrder(
        PostStopOrderRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .setQuantity(quantity)
          .setPrice(price)
          .setStopPrice(stopPrice)
          .setDirection(direction)
          .setAccountId(accountId)
          .setExpirationType(StopOrderExpirationType.STOP_ORDER_EXPIRATION_TYPE_GOOD_TILL_DATE)
          .setStopOrderType(type)
          .setExpireDate(DateUtils.instantToTimestamp(expireDate))
          .build())
      .getStopOrderId());
  }

  @Nonnull
  public String postStopSync(@Nonnull String instrumentId,
                             long quantity,
                             @Nonnull Quotation price,
                             @Nonnull Quotation stopPrice,
                             @Nonnull StopOrderDirection direction,
                             @Nonnull String accountId,
                             @Nonnull StopOrderType type,
                             @Nonnull StopOrderExpirationType expirationType,
                             @Nonnull TakeProfitType takeProfitType,
                             @Nonnull ru.tinkoff.piapi.contract.v1.PostStopOrderRequest.TrailingData trailingData,
                             @Nullable Instant expireDate) {
    ValidationUtils.checkReadonly(readonlyMode);
    ValidationUtils.checkSandbox(sandboxMode);

    return Helpers.unaryCall(() -> stopOrdersBlockingStub.postStopOrder(
        PostStopOrderRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .setQuantity(quantity)
          .setPrice(price)
          .setStopPrice(stopPrice)
          .setDirection(direction)
          .setAccountId(accountId)
          .setExpirationType(expirationType)
          .setStopOrderType(type)
          .setExpireDate((expireDate == null) ? Timestamp.getDefaultInstance() : DateUtils.instantToTimestamp(expireDate))
          .setTakeProfitType(takeProfitType)
          .setTrailingData(trailingData)
          .build())
      .getStopOrderId());
  }

  @Nonnull
  public String postStopOrderGoodTillCancelSync(@Nonnull String instrumentId,
                                                long quantity,
                                                @Nonnull Quotation price,
                                                @Nonnull Quotation stopPrice,
                                                @Nonnull StopOrderDirection direction,
                                                @Nonnull String accountId,
                                                @Nonnull StopOrderType type,
                                                @Nonnull PriceType priceType) {
    ValidationUtils.checkReadonly(readonlyMode);
    ValidationUtils.checkSandbox(sandboxMode);

    return Helpers.unaryCall(() -> stopOrdersBlockingStub.postStopOrder(
        PostStopOrderRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .setQuantity(quantity)
          .setPrice(price)
          .setStopPrice(stopPrice)
          .setDirection(direction)
          .setAccountId(accountId)
          .setExpirationType(StopOrderExpirationType.STOP_ORDER_EXPIRATION_TYPE_GOOD_TILL_CANCEL)
          .setStopOrderType(type)
          .setPriceType(priceType)
          .build())
      .getStopOrderId());
  }

  @Nonnull
  public String postStopOrderGoodTillDateSync(@Nonnull String instrumentId,
                                              long quantity,
                                              @Nonnull Quotation price,
                                              @Nonnull Quotation stopPrice,
                                              @Nonnull StopOrderDirection direction,
                                              @Nonnull String accountId,
                                              @Nonnull StopOrderType type,
                                              @Nonnull Instant expireDate,
                                              @Nonnull PriceType priceType) {
    ValidationUtils.checkReadonly(readonlyMode);
    ValidationUtils.checkSandbox(sandboxMode);

    return Helpers.unaryCall(() -> stopOrdersBlockingStub.postStopOrder(
        PostStopOrderRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .setQuantity(quantity)
          .setPrice(price)
          .setStopPrice(stopPrice)
          .setDirection(direction)
          .setAccountId(accountId)
          .setExpirationType(StopOrderExpirationType.STOP_ORDER_EXPIRATION_TYPE_GOOD_TILL_DATE)
          .setStopOrderType(type)
          .setExpireDate(DateUtils.instantToTimestamp(expireDate))
          .setPriceType(priceType)
          .build())
      .getStopOrderId());
  }

  @Nonnull
  public String postStopSync(@Nonnull String instrumentId,
                             long quantity,
                             @Nonnull Quotation price,
                             @Nonnull Quotation stopPrice,
                             @Nonnull StopOrderDirection direction,
                             @Nonnull String accountId,
                             @Nonnull StopOrderType type,
                             @Nonnull StopOrderExpirationType expirationType,
                             @Nonnull TakeProfitType takeProfitType,
                             @Nonnull ru.tinkoff.piapi.contract.v1.PostStopOrderRequest.TrailingData trailingData,
                             @Nullable Instant expireDate,
                             @Nonnull PriceType priceType) {
    ValidationUtils.checkReadonly(readonlyMode);
    ValidationUtils.checkSandbox(sandboxMode);

    return Helpers.unaryCall(() -> stopOrdersBlockingStub.postStopOrder(
        PostStopOrderRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .setQuantity(quantity)
          .setPrice(price)
          .setStopPrice(stopPrice)
          .setDirection(direction)
          .setAccountId(accountId)
          .setExpirationType(expirationType)
          .setStopOrderType(type)
          .setExpireDate((expireDate == null) ? Timestamp.getDefaultInstance() : DateUtils.instantToTimestamp(expireDate))
          .setTakeProfitType(takeProfitType)
          .setTrailingData(trailingData)
          .setPriceType(priceType)
          .build())
      .getStopOrderId());
  }

  @Nonnull
  public List<StopOrder> getStopOrdersSync(@Nonnull String accountId) {
    ValidationUtils.checkSandbox(sandboxMode);

    return Helpers.unaryCall(() -> stopOrdersBlockingStub.getStopOrders(
        GetStopOrdersRequest.newBuilder()
          .setAccountId(accountId)
          .build())
      .getStopOrdersList());
  }

  @Nonnull
  public List<StopOrder> getStopOrdersSync(@Nonnull String accountId,
                                           @Nullable Instant from,
                                           @Nullable Instant to,
                                           @Nullable StopOrderStatusOption status) {
    ValidationUtils.checkSandbox(sandboxMode);
    GetStopOrdersRequest.Builder requestBuilder = GetStopOrdersRequest.newBuilder()
      .setAccountId(accountId);
    if (from != null)
      requestBuilder.setFrom(DateUtils.instantToTimestamp(from));
    if (to != null)
      requestBuilder.setTo(DateUtils.instantToTimestamp(to));
    if (status != null && !status.equals(StopOrderStatusOption.STOP_ORDER_STATUS_UNSPECIFIED)) {
      requestBuilder.setStatus(status);
    }

    return Helpers.unaryCall(() -> stopOrdersBlockingStub.getStopOrders(
        requestBuilder.build())
      .getStopOrdersList());
  }

  @Nonnull
  public Instant cancelStopOrderSync(@Nonnull String accountId,
                                     @Nonnull String stopOrderId) {
    ValidationUtils.checkReadonly(readonlyMode);
    ValidationUtils.checkSandbox(sandboxMode);

    var responseTime = Helpers.unaryCall(() -> stopOrdersBlockingStub.cancelStopOrder(
        CancelStopOrderRequest.newBuilder()
          .setAccountId(accountId)
          .setStopOrderId(stopOrderId)
          .build())
      .getTime());

    return DateUtils.timestampToInstant(responseTime);
  }

  @Nonnull
  public CompletableFuture<String> postStopOrderGoodTillCancel(@Nonnull String instrumentId,
                                                               long quantity,
                                                               @Nonnull Quotation price,
                                                               @Nonnull Quotation stopPrice,
                                                               @Nonnull StopOrderDirection direction,
                                                               @Nonnull String accountId,
                                                               @Nonnull StopOrderType type) {
    ValidationUtils.checkReadonly(readonlyMode);
    ValidationUtils.checkSandbox(sandboxMode);

    return Helpers.<PostStopOrderResponse>unaryAsyncCall(
        observer -> stopOrdersStub.postStopOrder(
          PostStopOrderRequest.newBuilder()
            .setInstrumentId(instrumentId)
            .setQuantity(quantity)
            .setPrice(price)
            .setStopPrice(stopPrice)
            .setDirection(direction)
            .setAccountId(accountId)
            .setExpirationType(StopOrderExpirationType.STOP_ORDER_EXPIRATION_TYPE_GOOD_TILL_CANCEL)
            .setStopOrderType(type)
            .build(),
          observer))
      .thenApply(PostStopOrderResponse::getStopOrderId);
  }

  @Nonnull
  public CompletableFuture<String> postStopOrderGoodTillDate(@Nonnull String instrumentId,
                                                             long quantity,
                                                             @Nonnull Quotation price,
                                                             @Nonnull Quotation stopPrice,
                                                             @Nonnull StopOrderDirection direction,
                                                             @Nonnull String accountId,
                                                             @Nonnull StopOrderType type,
                                                             @Nonnull Instant expireDate) {
    ValidationUtils.checkReadonly(readonlyMode);
    ValidationUtils.checkSandbox(sandboxMode);

    return Helpers.<PostStopOrderResponse>unaryAsyncCall(
        observer -> stopOrdersStub.postStopOrder(
          PostStopOrderRequest.newBuilder()
            .setInstrumentId(instrumentId)
            .setQuantity(quantity)
            .setPrice(price)
            .setStopPrice(stopPrice)
            .setDirection(direction)
            .setAccountId(accountId)
            .setExpirationType(StopOrderExpirationType.STOP_ORDER_EXPIRATION_TYPE_GOOD_TILL_DATE)
            .setStopOrderType(type)
            .setExpireDate(DateUtils.instantToTimestamp(expireDate))
            .build(),
          observer))
      .thenApply(PostStopOrderResponse::getStopOrderId);
  }

  @Nonnull
  public CompletableFuture<String> postStopOrder(@Nonnull String instrumentId,
                                                 long quantity,
                                                 @Nonnull Quotation price,
                                                 @Nonnull Quotation stopPrice,
                                                 @Nonnull StopOrderDirection direction,
                                                 @Nonnull String accountId,
                                                 @Nonnull StopOrderType type,
                                                 @Nonnull StopOrderExpirationType expirationType,
                                                 @Nonnull TakeProfitType takeProfitType,
                                                 @Nonnull ru.tinkoff.piapi.contract.v1.PostStopOrderRequest.TrailingData trailingData,
                                                 @Nullable Instant expireDate) {
    ValidationUtils.checkReadonly(readonlyMode);
    ValidationUtils.checkSandbox(sandboxMode);

    return Helpers.<PostStopOrderResponse>unaryAsyncCall(observer -> stopOrdersStub.postStopOrder(
        PostStopOrderRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .setQuantity(quantity)
          .setPrice(price)
          .setStopPrice(stopPrice)
          .setDirection(direction)
          .setAccountId(accountId)
          .setExpirationType(expirationType)
          .setStopOrderType(type)
          .setExpireDate((expireDate == null) ? Timestamp.getDefaultInstance() : DateUtils.instantToTimestamp(expireDate))
          .setTakeProfitType(takeProfitType)
          .setTrailingData(trailingData)
          .build(), observer))
      .thenApply(PostStopOrderResponse::getStopOrderId);
  }

  @Nonnull
  public CompletableFuture<String> postStopOrderGoodTillCancel(@Nonnull String instrumentId,
                                                               long quantity,
                                                               @Nonnull Quotation price,
                                                               @Nonnull Quotation stopPrice,
                                                               @Nonnull StopOrderDirection direction,
                                                               @Nonnull String accountId,
                                                               @Nonnull StopOrderType type,
                                                               @Nonnull PriceType priceType) {
    ValidationUtils.checkReadonly(readonlyMode);
    ValidationUtils.checkSandbox(sandboxMode);

    return Helpers.<PostStopOrderResponse>unaryAsyncCall(
        observer -> stopOrdersStub.postStopOrder(
          PostStopOrderRequest.newBuilder()
            .setInstrumentId(instrumentId)
            .setQuantity(quantity)
            .setPrice(price)
            .setStopPrice(stopPrice)
            .setDirection(direction)
            .setAccountId(accountId)
            .setExpirationType(StopOrderExpirationType.STOP_ORDER_EXPIRATION_TYPE_GOOD_TILL_CANCEL)
            .setStopOrderType(type)
            .setPriceType(priceType)
            .build(),
          observer))
      .thenApply(PostStopOrderResponse::getStopOrderId);
  }

  @Nonnull
  public CompletableFuture<String> postStopOrderGoodTillDate(@Nonnull String instrumentId,
                                                             long quantity,
                                                             @Nonnull Quotation price,
                                                             @Nonnull Quotation stopPrice,
                                                             @Nonnull StopOrderDirection direction,
                                                             @Nonnull String accountId,
                                                             @Nonnull StopOrderType type,
                                                             @Nonnull Instant expireDate,
                                                             @Nonnull PriceType priceType) {
    ValidationUtils.checkReadonly(readonlyMode);
    ValidationUtils.checkSandbox(sandboxMode);

    return Helpers.<PostStopOrderResponse>unaryAsyncCall(
        observer -> stopOrdersStub.postStopOrder(
          PostStopOrderRequest.newBuilder()
            .setInstrumentId(instrumentId)
            .setQuantity(quantity)
            .setPrice(price)
            .setStopPrice(stopPrice)
            .setDirection(direction)
            .setAccountId(accountId)
            .setExpirationType(StopOrderExpirationType.STOP_ORDER_EXPIRATION_TYPE_GOOD_TILL_DATE)
            .setStopOrderType(type)
            .setExpireDate(DateUtils.instantToTimestamp(expireDate))
            .setPriceType(priceType)
            .build(),
          observer))
      .thenApply(PostStopOrderResponse::getStopOrderId);
  }

  @Nonnull
  public CompletableFuture<String> postStopOrder(@Nonnull String instrumentId,
                                                 long quantity,
                                                 @Nonnull Quotation price,
                                                 @Nonnull Quotation stopPrice,
                                                 @Nonnull StopOrderDirection direction,
                                                 @Nonnull String accountId,
                                                 @Nonnull StopOrderType type,
                                                 @Nonnull StopOrderExpirationType expirationType,
                                                 @Nonnull TakeProfitType takeProfitType,
                                                 @Nonnull ru.tinkoff.piapi.contract.v1.PostStopOrderRequest.TrailingData trailingData,
                                                 @Nullable Instant expireDate,
                                                 @Nonnull PriceType priceType) {
    ValidationUtils.checkReadonly(readonlyMode);
    ValidationUtils.checkSandbox(sandboxMode);

    return Helpers.<PostStopOrderResponse>unaryAsyncCall(observer -> stopOrdersStub.postStopOrder(
        PostStopOrderRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .setQuantity(quantity)
          .setPrice(price)
          .setStopPrice(stopPrice)
          .setDirection(direction)
          .setAccountId(accountId)
          .setExpirationType(expirationType)
          .setStopOrderType(type)
          .setExpireDate((expireDate == null) ? Timestamp.getDefaultInstance() : DateUtils.instantToTimestamp(expireDate))
          .setTakeProfitType(takeProfitType)
          .setTrailingData(trailingData)
          .setPriceType(priceType)
          .build(), observer))
      .thenApply(PostStopOrderResponse::getStopOrderId);
  }

  @Nonnull
  public CompletableFuture<List<StopOrder>> getStopOrders(@Nonnull String accountId) {
    ValidationUtils.checkSandbox(sandboxMode);

    return Helpers.<GetStopOrdersResponse>unaryAsyncCall(
        observer -> stopOrdersStub.getStopOrders(
          GetStopOrdersRequest.newBuilder()
            .setAccountId(accountId)
            .build(),
          observer))
      .thenApply(GetStopOrdersResponse::getStopOrdersList);
  }

  @Nonnull
  public CompletableFuture<List<StopOrder>> getStopOrders(@Nonnull String accountId,
                                                          @Nullable Instant from,
                                                          @Nullable Instant to,
                                                          @Nullable StopOrderStatusOption status) {
    ValidationUtils.checkSandbox(sandboxMode);
    GetStopOrdersRequest.Builder requestBuilder = GetStopOrdersRequest.newBuilder()
      .setAccountId(accountId);
    if (from != null)
      requestBuilder.setFrom(DateUtils.instantToTimestamp(from));
    if (to != null)
      requestBuilder.setTo(DateUtils.instantToTimestamp(to));
    if (status != null && !status.equals(StopOrderStatusOption.STOP_ORDER_STATUS_UNSPECIFIED)) {
      requestBuilder.setStatus(status);
    }
    return Helpers.<GetStopOrdersResponse>unaryAsyncCall(
        observer -> stopOrdersStub.getStopOrders(
          requestBuilder.build(),
          observer))
      .thenApply(GetStopOrdersResponse::getStopOrdersList);
  }

  @Nonnull
  public CompletableFuture<Instant> cancelStopOrder(@Nonnull String accountId,
                                                    @Nonnull String stopOrderId) {
    ValidationUtils.checkReadonly(readonlyMode);
    ValidationUtils.checkSandbox(sandboxMode);

    return Helpers.<CancelStopOrderResponse>unaryAsyncCall(
        observer -> stopOrdersStub.cancelStopOrder(
          CancelStopOrderRequest.newBuilder()
            .setAccountId(accountId)
            .setStopOrderId(stopOrderId)
            .build(),
          observer))
      .thenApply(response -> DateUtils.timestampToInstant(response.getTime()));
  }
}
