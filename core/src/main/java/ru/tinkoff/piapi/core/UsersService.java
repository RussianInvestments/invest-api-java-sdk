package ru.tinkoff.piapi.core;

import ru.tinkoff.piapi.contract.v1.AccountStatus;
import ru.tinkoff.piapi.contract.v1.AccountType;
import ru.tinkoff.piapi.core.utils.Helpers;
import ru.tinkoff.piapi.core.utils.ValidationUtils;
import ru.tinkoff.piapi.contract.v1.Account;
import ru.tinkoff.piapi.contract.v1.GetAccountsRequest;
import ru.tinkoff.piapi.contract.v1.GetAccountsResponse;
import ru.tinkoff.piapi.contract.v1.GetInfoRequest;
import ru.tinkoff.piapi.contract.v1.GetInfoResponse;
import ru.tinkoff.piapi.contract.v1.GetMarginAttributesRequest;
import ru.tinkoff.piapi.contract.v1.GetMarginAttributesResponse;
import ru.tinkoff.piapi.contract.v1.GetUserTariffRequest;
import ru.tinkoff.piapi.contract.v1.GetUserTariffResponse;
import ru.tinkoff.piapi.contract.v1.UsersServiceGrpc.UsersServiceBlockingStub;
import ru.tinkoff.piapi.contract.v1.UsersServiceGrpc.UsersServiceStub;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UsersService {
  private final UsersServiceBlockingStub userBlockingStub;
  private final UsersServiceStub userStub;
  private final boolean sandboxMode;

  UsersService(@Nonnull UsersServiceBlockingStub userBlockingStub,
               @Nonnull UsersServiceStub userStub,
               boolean sandboxMode) {
    this.sandboxMode = sandboxMode;
    this.userBlockingStub = userBlockingStub;
    this.userStub = userStub;
  }

  @Nonnull
  public List<Account> getAccountsSync() {
    return Helpers.unaryCall(() -> userBlockingStub.getAccounts(
        GetAccountsRequest.newBuilder()
          .build())
      .getAccountsList());
  }


  @Nonnull
  public CompletableFuture<List<Account>> getAccounts(AccountStatus status) {
    return Helpers.<GetAccountsResponse>unaryAsyncCall(
        observer -> userStub.getAccounts(
          GetAccountsRequest.newBuilder()
            .setStatus(status).build(),
          observer))
      .thenApply(GetAccountsResponse::getAccountsList);
  }

  @Nonnull
  public List<Account> getAccountsSync(AccountStatus status) {
    return Helpers.unaryCall(() -> userBlockingStub.getAccounts(
        GetAccountsRequest.newBuilder().setStatus(status)
          .build())
      .getAccountsList());
  }


  @Nonnull
  public CompletableFuture<List<Account>> getAccounts() {
    return Helpers.<GetAccountsResponse>unaryAsyncCall(
        observer -> userStub.getAccounts(
          GetAccountsRequest.newBuilder().build(),
          observer))
      .thenApply(GetAccountsResponse::getAccountsList);
  }

  @Nonnull
  public GetMarginAttributesResponse getMarginAttributesSync(@Nonnull String accountId) {
    return Helpers.unaryCall(() -> userBlockingStub.getMarginAttributes(
      GetMarginAttributesRequest.newBuilder().setAccountId(accountId).build()));
  }


  @Nonnull
  public CompletableFuture<GetMarginAttributesResponse> getMarginAttributes(@Nonnull String accountId) {
    ValidationUtils.checkSandbox(sandboxMode);

    return Helpers.unaryAsyncCall(
      observer -> userStub.getMarginAttributes(
        GetMarginAttributesRequest.newBuilder().setAccountId(accountId).build(),
        observer));
  }

  @Nonnull
  public GetUserTariffResponse getUserTariffSync() {
    return Helpers.unaryCall(() -> userBlockingStub.getUserTariff(GetUserTariffRequest.newBuilder().build()));
  }

  @Nonnull
  public CompletableFuture<GetUserTariffResponse> getUserTariff() {
    return Helpers.unaryAsyncCall(
      observer -> userStub.getUserTariff(
        GetUserTariffRequest.newBuilder().build(),
        observer));
  }

  @Nonnull
  public GetInfoResponse getInfoSync() {
    return Helpers.unaryCall(() -> userBlockingStub.getInfo(GetInfoRequest.newBuilder().build()));
  }

  @Nonnull
  public CompletableFuture<GetInfoResponse> getInfo() {
    return Helpers.unaryAsyncCall(
      observer -> userStub.getInfo(
        GetInfoRequest.newBuilder().build(),
        observer));
  }
}
