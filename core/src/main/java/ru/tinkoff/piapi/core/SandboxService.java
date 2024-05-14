package ru.tinkoff.piapi.core;

import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.utils.DateUtils;
import ru.tinkoff.piapi.core.utils.Helpers;
import ru.tinkoff.piapi.contract.v1.SandboxServiceGrpc.SandboxServiceBlockingStub;
import ru.tinkoff.piapi.contract.v1.SandboxServiceGrpc.SandboxServiceStub;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SandboxService {
  private final SandboxServiceBlockingStub sandboxBlockingStub;
  private final SandboxServiceStub sandboxStub;

  SandboxService(@Nonnull SandboxServiceBlockingStub sandboxBlockingStub,
                 @Nonnull SandboxServiceStub sandboxStub) {
    this.sandboxBlockingStub = sandboxBlockingStub;
    this.sandboxStub = sandboxStub;
  }

  @Nonnull
  public String openAccountSync() {
    return openAccountSync(null);
  }

  @Nonnull
  public String openAccountSync(@Nullable String name) {
    OpenSandboxAccountRequest.Builder request = OpenSandboxAccountRequest.newBuilder();
    if (name != null) {
      request.setName(name);
    }
    return Helpers.unaryCall(() -> sandboxBlockingStub.openSandboxAccount(
        request.build())
      .getAccountId());
  }

  @Nonnull
  public List<Account> getAccountsSync() {
    return Helpers.unaryCall(() -> sandboxBlockingStub.getSandboxAccounts(
        GetAccountsRequest.newBuilder()
          .build())
      .getAccountsList());
  }

  public void closeAccountSync(@Nonnull String accountId) {
    Helpers.unaryCall(() -> sandboxBlockingStub.closeSandboxAccount(
      CloseSandboxAccountRequest.newBuilder()
        .setAccountId(accountId)
        .build()));
  }

  @Nonnull
  public PostOrderResponse postOrderSync(@Nonnull String instrumentId,
                                         long quantity,
                                         @Nonnull Quotation price,
                                         @Nonnull OrderDirection direction,
                                         @Nonnull String accountId,
                                         @Nonnull OrderType type,
                                         @Nonnull String orderId) {
    return postOrderSync(instrumentId, quantity, price, direction, accountId, type, orderId, PriceType.PRICE_TYPE_UNSPECIFIED);
  }

  @Nonnull
  public PostOrderResponse postOrderSync(@Nonnull String instrumentId,
                                         long quantity,
                                         @Nonnull Quotation price,
                                         @Nonnull OrderDirection direction,
                                         @Nonnull String accountId,
                                         @Nonnull OrderType type,
                                         @Nonnull String orderId,
                                         @Nonnull PriceType priceType) {
    return Helpers.unaryCall(() -> sandboxBlockingStub.postSandboxOrder(
      PostOrderRequest.newBuilder()
        .setInstrumentId(instrumentId)
        .setQuantity(quantity)
        .setPrice(price)
        .setDirection(direction)
        .setAccountId(accountId)
        .setOrderType(type)
        .setOrderId(Helpers.preprocessInputOrderId(orderId))
        .setPriceType(priceType)
        .build()));
  }

  @Nonnull
  public List<OrderState> getOrdersSync(@Nonnull String accountId) {
    return Helpers.unaryCall(() -> sandboxBlockingStub.getSandboxOrders(
        GetOrdersRequest.newBuilder()
          .setAccountId(accountId)
          .build())
      .getOrdersList());
  }

  @Nonnull
  public Instant cancelOrderSync(@Nonnull String accountId,
                                 @Nonnull String orderId) {
    var responseTime = Helpers.unaryCall(() -> sandboxBlockingStub.cancelSandboxOrder(
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
    return Helpers.unaryCall(() -> sandboxBlockingStub.getSandboxOrderState(
      GetOrderStateRequest.newBuilder()
        .setAccountId(accountId)
        .setOrderId(orderId)
        .build()));
  }

  public OrderState getOrderStateSync(@Nonnull String accountId,
                                      @Nonnull String orderId,
                                      @Nonnull PriceType priceType) {
    return Helpers.unaryCall(() -> sandboxBlockingStub.getSandboxOrderState(
      GetOrderStateRequest.newBuilder()
        .setAccountId(accountId)
        .setOrderId(orderId)
        .setPriceType(priceType)
        .build()));
  }

  @Nonnull
  public PositionsResponse getPositionsSync(@Nonnull String accountId) {
    return Helpers.unaryCall(() -> sandboxBlockingStub.getSandboxPositions(
      PositionsRequest.newBuilder().setAccountId(accountId).build()));
  }

  @Nonnull
  public List<Operation> getOperationsSync(@Nonnull String accountId,
                                           @Nonnull Instant from,
                                           @Nonnull Instant to,
                                           @Nonnull OperationState operationState,
                                           @Nullable String figi) {
    return Helpers.unaryCall(() -> sandboxBlockingStub.getSandboxOperations(
        OperationsRequest.newBuilder()
          .setAccountId(accountId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .setState(operationState)
          .setFigi(figi == null ? "" : figi)
          .build())
      .getOperationsList());
  }

  @Nonnull
  public PortfolioResponse getPortfolioSync(@Nonnull String accountId) {
    return Helpers.unaryCall(() -> sandboxBlockingStub.getSandboxPortfolio(
      PortfolioRequest.newBuilder().setAccountId(accountId).build()));
  }

  @Nonnull
  public MoneyValue payInSync(@Nonnull String accountId, @Nonnull MoneyValue moneyValue) {
    return Helpers.unaryCall(() -> sandboxBlockingStub.sandboxPayIn(
        SandboxPayInRequest.newBuilder()
          .setAccountId(accountId)
          .setAmount(moneyValue)
          .build())
      .getBalance());
  }

  @Nonnull
  public CompletableFuture<String> openAccount() {
    return openAccount(null);
  }
  @Nonnull
  public CompletableFuture<String> openAccount(@Nullable String name) {
    OpenSandboxAccountRequest.Builder request = OpenSandboxAccountRequest.newBuilder();
    if (name != null) {
      request.setName(name);
    }
    return Helpers.<OpenSandboxAccountResponse>unaryAsyncCall(
        observer -> sandboxStub.openSandboxAccount(
          request.build(),
          observer))
      .thenApply(OpenSandboxAccountResponse::getAccountId);
  }

  @Nonnull
  public CompletableFuture<List<Account>> getAccounts() {
    return Helpers.<GetAccountsResponse>unaryAsyncCall(
        observer -> sandboxStub.getSandboxAccounts(
          GetAccountsRequest.newBuilder().build(),
          observer))
      .thenApply(GetAccountsResponse::getAccountsList);
  }

  @Nonnull
  public CompletableFuture<Void> closeAccount(@Nonnull String accountId) {
    return Helpers.<CloseSandboxAccountResponse>unaryAsyncCall(
        observer -> sandboxStub.closeSandboxAccount(
          CloseSandboxAccountRequest.newBuilder()
            .setAccountId(accountId)
            .build(),
          observer))
      .thenApply(r -> null);
  }

  public CompletableFuture<PostOrderResponse> postOrder(@Nonnull String instrumentId,
                                                        long quantity,
                                                        @Nonnull Quotation price,
                                                        @Nonnull OrderDirection direction,
                                                        @Nonnull String accountId,
                                                        @Nonnull OrderType type,
                                                        @Nonnull String orderId) {
    return postOrder(instrumentId, quantity, price, direction, accountId, type, orderId, PriceType.PRICE_TYPE_UNSPECIFIED);
  }

  @Nonnull
  public CompletableFuture<PostOrderResponse> postOrder(@Nonnull String instrumentId,
                                                        long quantity,
                                                        @Nonnull Quotation price,
                                                        @Nonnull OrderDirection direction,
                                                        @Nonnull String accountId,
                                                        @Nonnull OrderType type,
                                                        @Nonnull String orderId,
                                                        @Nonnull PriceType priceType) {
    return Helpers.unaryAsyncCall(
      observer -> sandboxStub.postSandboxOrder(
        PostOrderRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .setQuantity(quantity)
          .setPrice(price)
          .setDirection(direction)
          .setAccountId(accountId)
          .setOrderType(type)
          .setOrderId(Helpers.preprocessInputOrderId(orderId))
          .setPriceType(priceType)
          .build(),
        observer));
  }

  @Nonnull
  public CompletableFuture<List<OrderState>> getOrders(@Nonnull String accountId) {
    return Helpers.<GetOrdersResponse>unaryAsyncCall(
        observer -> sandboxStub.getSandboxOrders(
          GetOrdersRequest.newBuilder()
            .setAccountId(accountId)
            .build(),
          observer))
      .thenApply(GetOrdersResponse::getOrdersList);
  }

  @Nonnull
  public CompletableFuture<Instant> cancelOrder(@Nonnull String accountId,
                                                @Nonnull String orderId) {
    return Helpers.<CancelOrderResponse>unaryAsyncCall(
        observer -> sandboxStub.cancelSandboxOrder(
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
      observer -> sandboxStub.getSandboxOrderState(
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
      observer -> sandboxStub.getSandboxOrderState(
        GetOrderStateRequest.newBuilder()
          .setAccountId(accountId)
          .setOrderId(orderId)
          .setPriceType(priceType)
          .build(),
        observer));
  }

  @Nonnull
  public CompletableFuture<PositionsResponse> getPositions(@Nonnull String accountId) {
    return Helpers.unaryAsyncCall(
      observer -> sandboxStub.getSandboxPositions(
        PositionsRequest.newBuilder().setAccountId(accountId).build(),
        observer));
  }

  @Nonnull
  public CompletableFuture<List<Operation>> getOperations(@Nonnull String accountId,
                                                          @Nonnull Instant from,
                                                          @Nonnull Instant to,
                                                          @Nonnull OperationState operationState,
                                                          @Nullable String figi) {
    return Helpers.<OperationsResponse>unaryAsyncCall(
        observer -> sandboxStub.getSandboxOperations(
          OperationsRequest.newBuilder()
            .setAccountId(accountId)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .setState(operationState)
            .setFigi(figi == null ? "" : figi)
            .build(),
          observer))
      .thenApply(OperationsResponse::getOperationsList);
  }

  @Nonnull
  public CompletableFuture<PortfolioResponse> getPortfolio(@Nonnull String accountId) {
    return Helpers.unaryAsyncCall(
      observer -> sandboxStub.getSandboxPortfolio(
        PortfolioRequest.newBuilder().setAccountId(accountId).build(),
        observer));
  }

  @Nonnull
  public CompletableFuture<MoneyValue> payIn(@Nonnull String accountId,
                                             @Nonnull MoneyValue moneyValue) {
    return Helpers.<SandboxPayInResponse>unaryAsyncCall(
        observer -> sandboxStub.sandboxPayIn(
          SandboxPayInRequest.newBuilder()
            .setAccountId(accountId)
            .setAmount(moneyValue)
            .build(),
          observer))
      .thenApply(SandboxPayInResponse::getBalance);
  }

  /** Получение (синхронное) списка операций по счёту с пагинацией.
   *
   * @param accountId Идентификатор счёта клиента
   * @param from Начало периода (по UTC)
   * @param to Окончание периода (по UTC)
   * @param cursor Идентификатор элемента, с которого начать формировать ответ
   * @param limit Лимит количества операций
   * @param operationState Статус запрашиваемых операций, возможные значения
   * @param instrumentId Идентификатор инструмента (Figi инструмента или uid инструмента)
   * @param withoutCommission Флаг возвращать ли комиссии, по умолчанию false
   * @param withoutTrades Флаг ответ без сделок, по умолчанию false
   * @param withoutOvernights Флаг не показывать overnight операций, по умолчанию false
   * @param operationTypes Тип операции. Принимает значение из списка
   * @return Список операций по счёту с пагинацией
   */
  @Nonnull
  public GetOperationsByCursorResponse getOperationByCursorSync(@Nonnull String accountId,
                                                                @Nonnull Instant from,
                                                                @Nonnull Instant to,
                                                                @Nullable String cursor,
                                                                @Nullable Integer limit,
                                                                @Nullable OperationState operationState,
                                                                @Nullable String instrumentId,
                                                                @Nullable Boolean withoutCommission,
                                                                @Nullable Boolean withoutTrades,
                                                                @Nullable Boolean withoutOvernights,
                                                                @Nullable List<OperationType> operationTypes
  ) {

    var request = GetOperationsByCursorRequest.newBuilder()
      .setAccountId(accountId)
      .setFrom(DateUtils.instantToTimestamp(from))
      .setTo(DateUtils.instantToTimestamp(to))
      .setCursor(cursor == null ? "" : cursor)
      .setLimit(limit == null ? 0 : limit)
      .setState(operationState == null ? OperationState.OPERATION_STATE_UNSPECIFIED : operationState)
      .setInstrumentId(instrumentId == null ? "" : instrumentId)
      .setWithoutCommissions(withoutCommission != null && withoutCommission)
      .setWithoutOvernights(withoutOvernights != null && withoutOvernights)
      .setWithoutTrades(withoutTrades != null && withoutTrades)
      .addAllOperationTypes(operationTypes == null ? Collections.emptyList() : operationTypes)
      .build();
    return Helpers.unaryCall(() -> sandboxBlockingStub.getSandboxOperationsByCursor(request));
  }

  /** Получение (синхронное) списка операций по счёту с пагинацией.
   *
   * @param accountId Идентификатор счёта клиента
   * @param from Начало периода (по UTC)
   * @param to Окончание периода (по UTC)
   * @return Список операций по счёту с пагинацией
   */
  @Nonnull
  public GetOperationsByCursorResponse getOperationByCursorSync(@Nonnull String accountId,
                                                                @Nonnull Instant from,
                                                                @Nonnull Instant to
  ) {
    return getOperationByCursorSync(accountId, from, to, null, null, null, null,
      false, false, false, null);
  }

  /** Получение (асинхронное) списка операций по счёту с пагинацией.
   *
   * @param accountId Идентификатор счёта клиента
   * @param from Начало периода (по UTC)
   * @param to Окончание периода (по UTC)
   * @param cursor Идентификатор элемента, с которого начать формировать ответ
   * @param limit Лимит количества операций
   * @param operationState Статус запрашиваемых операций, возможные значения
   * @param instrumentId Идентификатор инструмента (Figi инструмента или uid инструмента)
   * @param withoutCommission Флаг возвращать ли комиссии, по умолчанию false
   * @param withoutTrades Флаг ответ без сделок, по умолчанию false
   * @param withoutOvernights Флаг не показывать overnight операций, по умолчанию false
   * @param operationTypes Тип операции. Принимает значение из списка
   * @return Список операций по счёту с пагинацией
   */
  @Nonnull
  public CompletableFuture<GetOperationsByCursorResponse> getOperationByCursor (@Nonnull String accountId,
                                                                                @Nonnull Instant from,
                                                                                @Nonnull Instant to,
                                                                                @Nullable String cursor,
                                                                                @Nullable Integer limit,
                                                                                @Nullable OperationState operationState,
                                                                                @Nullable String instrumentId,
                                                                                @Nullable Boolean withoutCommission,
                                                                                @Nullable Boolean withoutTrades,
                                                                                @Nullable Boolean withoutOvernights,
                                                                                @Nullable List<OperationType> operationTypes) {
    var request = GetOperationsByCursorRequest.newBuilder()
      .setAccountId(accountId)
      .setFrom(DateUtils.instantToTimestamp(from))
      .setTo(DateUtils.instantToTimestamp(to))
      .setCursor(cursor == null ? "" : cursor)
      .setLimit(limit == null ? 0 : limit)
      .setState(operationState == null ? OperationState.OPERATION_STATE_UNSPECIFIED : operationState)
      .setInstrumentId(instrumentId == null ? "" : instrumentId)
      .setWithoutCommissions(withoutCommission != null && withoutCommission)
      .setWithoutOvernights(withoutOvernights != null && withoutOvernights)
      .setWithoutTrades(withoutTrades != null && withoutTrades)
      .addAllOperationTypes(operationTypes == null ? Collections.emptyList() : operationTypes)
      .build();
    return Helpers.unaryAsyncCall(
      observer -> sandboxStub.getSandboxOperationsByCursor(request, observer));
  }

  /** Получение (асинхронное) списка операций по счёту с пагинацией.
   *
   * @param accountId Идентификатор счёта клиента
   * @param from Начало периода (по UTC)
   * @param to Окончание периода (по UTC)
   * @return Список операций по счёту с пагинацией
   */
  @Nonnull
  public CompletableFuture<GetOperationsByCursorResponse> getOperationByCursor(@Nonnull String accountId,
                                                                               @Nonnull Instant from,
                                                                               @Nonnull Instant to
  ) {
    return getOperationByCursor(accountId, from, to, null, null, null, null,
      false, false, false, null);
  }
}
