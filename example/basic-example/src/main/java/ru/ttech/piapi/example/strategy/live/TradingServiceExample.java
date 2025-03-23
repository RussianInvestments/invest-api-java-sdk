package ru.ttech.piapi.example.strategy.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Bar;
import ru.tinkoff.piapi.contract.v1.Account;
import ru.tinkoff.piapi.contract.v1.AccountStatus;
import ru.tinkoff.piapi.contract.v1.AccountType;
import ru.tinkoff.piapi.contract.v1.CancelOrderRequest;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.GetAccountsRequest;
import ru.tinkoff.piapi.contract.v1.GetMaxLotsRequest;
import ru.tinkoff.piapi.contract.v1.GetOrdersRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentIdType;
import ru.tinkoff.piapi.contract.v1.InstrumentRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderIdType;
import ru.tinkoff.piapi.contract.v1.OrderType;
import ru.tinkoff.piapi.contract.v1.OrdersServiceGrpc;
import ru.tinkoff.piapi.contract.v1.PostOrderRequest;
import ru.tinkoff.piapi.contract.v1.SandboxPayInRequest;
import ru.tinkoff.piapi.contract.v1.SandboxServiceGrpc;
import ru.tinkoff.piapi.contract.v1.UsersServiceGrpc;
import ru.tinkoff.piapi.contract.v1.WithdrawLimitsRequest;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.SyncStubWrapper;
import ru.ttech.piapi.core.helpers.NumberMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Данный класс является примером реализации сервиса, который отвечает за торговлю на бирже согласно сигналам по стратегии
 */
public class TradingServiceExample {

  private static final Logger log = LoggerFactory.getLogger(TradingServiceExample.class);

  private final ConnectorConfiguration configuration;
  private final SyncStubWrapper<UsersServiceGrpc.UsersServiceBlockingStub> userService;
  private final SyncStubWrapper<InstrumentsServiceGrpc.InstrumentsServiceBlockingStub> instrumentsService;
  private final SyncStubWrapper<OrdersServiceGrpc.OrdersServiceBlockingStub> ordersService;
  private final SyncStubWrapper<SandboxServiceGrpc.SandboxServiceBlockingStub> sandboxService;
  private String tradingAccountId;
  private final BigDecimal sandboxBalance;
  private final int instrumentLots;

  public TradingServiceExample(
    ServiceStubFactory serviceStubFactory,
    BigDecimal sandboxBalance,
    int instrumentLots
  ) {
    this.configuration = serviceStubFactory.getConfiguration();
    this.sandboxBalance = sandboxBalance;
    this.instrumentLots = instrumentLots;
    this.userService = serviceStubFactory.newSyncService(UsersServiceGrpc::newBlockingStub);
    this.instrumentsService = serviceStubFactory.newSyncService(InstrumentsServiceGrpc::newBlockingStub);
    this.ordersService = serviceStubFactory.newSyncService(OrdersServiceGrpc::newBlockingStub);
    this.sandboxService = serviceStubFactory.newSyncService(SandboxServiceGrpc::newBlockingStub);
    init();
  }

  /**
   * Вызывается при входе по стратегии
   *
   * @param instrument инструмент
   * @param bar бар, на котором произошёл сигнал на вход по стратегии
   */
  public void onStrategyEnter(CandleInstrument instrument, Bar bar) {
    String instrumentId = instrument.getInstrumentId();
    var closePrice = bar.getClosePrice().bigDecimalValue();
    cancelOpenedOrdersForInstrument(instrumentId);
    var price = getInstrumentPrice(instrumentId, closePrice, OrderDirection.ORDER_DIRECTION_BUY);
    long quantity = Math.min(instrumentLots, getMaxBuyLots(instrumentId, price));
    if (quantity < instrumentLots) {
      throw new IllegalStateException("Недостаточно лотов для открытия сделки");
    }
    postLimitOrder(instrument.getInstrumentId(), OrderDirection.ORDER_DIRECTION_BUY, quantity, price);
    log.info("Вход по стратегии: {} по цене: {} (лотов: {})", instrument.getInstrumentId(), price, quantity);
  }

  /**
   * Вызывается при выходе по стратегии
   *
   * @param instrument инструмент
   * @param bar бар, на котором произошёл сигнал на выход по стратегии
   */
  public void onStrategyExit(CandleInstrument instrument, Bar bar) {
    String instrumentId = instrument.getInstrumentId();
    var closePrice = bar.getClosePrice().bigDecimalValue();
    cancelOpenedOrdersForInstrument(instrumentId);
    long quantity = getMaxSellLots(instrumentId);
    var price = getInstrumentPrice(instrumentId, closePrice, OrderDirection.ORDER_DIRECTION_SELL);
    if (quantity <= 0) {
      throw new IllegalStateException("Недостаточно лотов для открытия сделки");
    }
    postLimitOrder(instrument.getInstrumentId(), OrderDirection.ORDER_DIRECTION_SELL, quantity, price);
    log.info("Выход по стратегии: {} по цене: {} (лотов: {})", instrument.getInstrumentId(), price, quantity);
  }

  /**
   * Метод для выставления лимитной заявки на покупку/продажу инструмента
   *
   * @param instrumentId - идентификатор инструмента
   * @param direction - направление сделки
   * @param quantity - количество лотов инструмента
   * @param price - цена инструмента
   */
  private void postLimitOrder(String instrumentId, OrderDirection direction, long quantity, BigDecimal price) {
    if (Optional.ofNullable(tradingAccountId).isEmpty()) {
      throw new IllegalStateException("Нельзя выставить ордер, так как не указан брокерский счет");
    }
    var postOrderRequest = PostOrderRequest.newBuilder()
      .setAccountId(tradingAccountId)
      .setInstrumentId(instrumentId)
      .setDirection(direction)
      .setQuantity(quantity)
      .setPrice(NumberMapper.bigDecimalToQuotation(price))
      .setOrderType(OrderType.ORDER_TYPE_LIMIT)
      .build();
    ordersService.callSyncMethod(stub -> stub.postOrder(postOrderRequest));
  }

  /**
   * Метод для отмены всех открытых ордеров по инструменту
   *
   * @param instrumentId инструмент
   */
  private void cancelOpenedOrdersForInstrument(String instrumentId) {
    var ordersRequest = GetOrdersRequest.newBuilder()
      .setAccountId(tradingAccountId)
      .build();
    var cancelOrderRequestBuilder = CancelOrderRequest.newBuilder()
      .setAccountId(tradingAccountId)
      .setOrderIdType(OrderIdType.ORDER_ID_TYPE_EXCHANGE);
    var ordersResponse = ordersService.callSyncMethod(stub -> stub.getOrders(ordersRequest));
    ordersResponse.getOrdersList().stream()
      .filter(orderState -> orderState.getInstrumentUid().equals(instrumentId))
      .forEach(order -> ordersService.callSyncMethod(stub ->
        stub.cancelOrder(cancelOrderRequestBuilder.setOrderId(order.getOrderId()).build())
      ));
    log.info("Отменены все открытые ордера по инструменту {}", instrumentId);
  }

  /**
   * Метод для получения максимального количества лотов, доступных к покупке
   *
   * @param instrumentId идентификатор инструмента
   * @param price цена инструмента
   * @return количество лотов инструмента
   */
  private long getMaxBuyLots(String instrumentId, BigDecimal price) {
    var getMaxLotsRequest = GetMaxLotsRequest.newBuilder()
      .setAccountId(tradingAccountId)
      .setInstrumentId(instrumentId)
      .setPrice(NumberMapper.bigDecimalToQuotation(price))
      .build();
    var response = ordersService.callSyncMethod(stub -> stub.getMaxLots(getMaxLotsRequest));
    return response.getBuyLimits().getBuyMaxLots();
  }

  /**
   * Метод для получения максимального количества лотов, доступных к продаже
   *
   * @param instrumentId идентификатор инструмента
   * @return количество лотов инструмента
   */
  private long getMaxSellLots(String instrumentId) {
    var getMaxLotsRequest = GetMaxLotsRequest.newBuilder()
      .setAccountId(tradingAccountId)
      .setInstrumentId(instrumentId)
      .build();
    var response = ordersService.callSyncMethod(stub -> stub.getMaxLots(getMaxLotsRequest));
    return response.getSellLimits().getSellMaxLots();
  }

  /**
   * Метод для расчёта цены инструмента исходя из направления сделки
   *
   * @param instrumentId идентификатор инструмента
   * @param price цена инструмента
   * @param direction направление сделки
   * @return цена инструмента
   */
  private BigDecimal getInstrumentPrice(String instrumentId, BigDecimal price, OrderDirection direction) {
    var minPriceIncrement = getMinPriceIncrement(instrumentId);
    switch (direction) {
      case ORDER_DIRECTION_BUY:
        return roundDownPrice(price, minPriceIncrement);
      case ORDER_DIRECTION_SELL:
        return roundUpPrice(price, minPriceIncrement);
      default:
        throw new IllegalArgumentException("Неизвестное направление ордера");
    }
  }

  /**
   * Метод для получения минимального шага цены по инструменту
   *
   * @param instrumentId идентификатор инструмента
   * @return минимальный шаг цены
   */
  private BigDecimal getMinPriceIncrement(String instrumentId) {
    var instrumentRequest = InstrumentRequest.newBuilder()
      .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_UID)
      .setId(instrumentId)
      .build();
    var instrumentResponse = instrumentsService.callSyncMethod(stub -> stub.getInstrumentBy(instrumentRequest));
    return NumberMapper.quotationToBigDecimal(instrumentResponse.getInstrument().getMinPriceIncrement());
  }

  /**
   * Метод для округления цены инструмента вверх
   *
   * @param price цена инструмента
   * @param minPriceIncrement минимальный шаг цены инструмента
   * @return округленная цена инструмента
   */
  private BigDecimal roundUpPrice(BigDecimal price, BigDecimal minPriceIncrement) {
    return price.divide(minPriceIncrement, 0, RoundingMode.UP).multiply(minPriceIncrement);
  }

  /**
   * Метод для округления цены инструмента вниз
   *
   * @param price цена инструмента
   * @param minPriceIncrement минимальный шаг цены инструмента
   * @return округленная цена инструмента
   */
  private BigDecimal roundDownPrice(BigDecimal price, BigDecimal minPriceIncrement) {
    return price.divide(minPriceIncrement, 0, RoundingMode.DOWN).multiply(minPriceIncrement);
  }

  /**
   * Метод инициализации сервиса. Проверяет, что у пользователя есть брокерский счёт.
   * Если торговля запущена на песочние, то пополняет её баланс до целевого
   */
  private void init() {
    var accountsRequest = GetAccountsRequest.newBuilder()
      .setStatus(AccountStatus.ACCOUNT_STATUS_OPEN)
      .build();
    var accountsResponse = userService.callSyncMethod(stub -> stub.getAccounts(accountsRequest));
    tradingAccountId = accountsResponse.getAccountsList().stream()
      .filter(acc -> acc.getType() == AccountType.ACCOUNT_TYPE_TINKOFF)
      .findFirst()
      .map(Account::getId)
      .orElseThrow(() -> new IllegalStateException("Не найден открытый брокерский счет"));
    log.info("Брокерский счет: {}", tradingAccountId);
    if (configuration.isSandboxEnabled()) {
      log.info("Торговля запущена на песочнице");
      payInSandbox();
    } else {
      log.info("Торговля запущена на реальном рынке");
    }
  }

  /**
   * Метод для пополнения баланса песочницы при инициализации сервиса
   */
  private void payInSandbox() {
    var balanceRequest = WithdrawLimitsRequest.newBuilder().setAccountId(tradingAccountId).build();
    var balanceResponse = sandboxService.callSyncMethod(stub -> stub.getSandboxWithdrawLimits(balanceRequest));
    var balance = balanceResponse.getMoneyList().stream().filter(moneyValue -> moneyValue.getCurrency().equals("rub"))
      .findFirst()
      .map(NumberMapper::moneyValueToBigDecimal)
      .orElse(BigDecimal.ZERO);
    log.info("Баланс: {} (настройка: {})", balance, sandboxBalance);
    if (balance.compareTo(sandboxBalance) < 0) {
      var amount = sandboxBalance.subtract(balance);
      var payInRequest = SandboxPayInRequest.newBuilder()
        .setAccountId(tradingAccountId)
        .setAmount(NumberMapper.bigDecimalToMoneyValue(amount, "rub"))
        .build();
      sandboxService.callSyncMethod(stub -> stub.sandboxPayIn(payInRequest));
      log.info("Баланс песочницы пополнен на сумму: {} руб.", amount);
    }
  }
}
