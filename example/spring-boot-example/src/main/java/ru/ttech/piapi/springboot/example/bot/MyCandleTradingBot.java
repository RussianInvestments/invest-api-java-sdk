package ru.ttech.piapi.springboot.example.bot;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import ru.tinkoff.piapi.contract.v1.Account;
import ru.tinkoff.piapi.contract.v1.AccountStatus;
import ru.tinkoff.piapi.contract.v1.AccountType;
import ru.tinkoff.piapi.contract.v1.CancelOrderRequest;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.GetAccountsRequest;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;
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
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.tinkoff.piapi.contract.v1.UsersServiceGrpc;
import ru.tinkoff.piapi.contract.v1.WithdrawLimitsRequest;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.SyncStubWrapper;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.springboot.bot.CandleTradingBot;
import ru.ttech.piapi.springboot.example.config.TradingProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Component
public class MyCandleTradingBot implements CandleTradingBot {

  private final TradingProperties properties;
  private final ConnectorConfiguration configuration;
  private final SyncStubWrapper<UsersServiceGrpc.UsersServiceBlockingStub> userService;
  private final SyncStubWrapper<InstrumentsServiceGrpc.InstrumentsServiceBlockingStub> instrumentsService;
  private final SyncStubWrapper<OrdersServiceGrpc.OrdersServiceBlockingStub> ordersService;
  private final SyncStubWrapper<SandboxServiceGrpc.SandboxServiceBlockingStub> sandboxService;
  private String tradingAccountId;

  public MyCandleTradingBot(
    ConnectorConfiguration configuration,
    TradingProperties properties,
    ServiceStubFactory serviceStubFactory
  ) {
    this.configuration = configuration;
    this.properties = properties;
    this.userService = serviceStubFactory.newSyncService(UsersServiceGrpc::newBlockingStub);
    this.instrumentsService = serviceStubFactory.newSyncService(InstrumentsServiceGrpc::newBlockingStub);
    this.ordersService = serviceStubFactory.newSyncService(OrdersServiceGrpc::newBlockingStub);
    this.sandboxService = serviceStubFactory.newSyncService(SandboxServiceGrpc::newBlockingStub);
  }

  @PostConstruct
  public void init() {
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
      payInSandbox();
    }
  }

  @Override
  public GetCandlesRequest.CandleSource getCandleSource() {
    return GetCandlesRequest.CandleSource.CANDLE_SOURCE_INCLUDE_WEEKEND;
  }

  @Override
  public int getWarmupLength() {
    return 100;
  }

  @Override
  public Map<CandleInstrument, Function<BarSeries, Strategy>> setStrategies() {
    var vtbrShare = CandleInstrument.newBuilder()
      .setInstrumentId("8e2b0325-0292-4654-8a18-4f63ed3b0e09")
      .setInterval(SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE)
      .build();
    return Map.of(
      vtbrShare, createStrategy(5, 10)
    );
  }

  @Override
  public void onStrategyEnterAction(CandleInstrument instrument, Bar bar) {
    String instrumentId = instrument.getInstrumentId();
    var closePrice = bar.getClosePrice().bigDecimalValue();
    cancelOpenedOrdersForInstrument(instrumentId);
    var price = getLotPrice(instrumentId, closePrice, OrderDirection.ORDER_DIRECTION_BUY);
    long quantity = Math.min(properties.getLots(), getMaxBuyLots(instrumentId, price));
    if (quantity < properties.getLots()) {
      throw new IllegalStateException("Недостаточно лотов для открытия сделки");
    }
    postLimitOrder(instrument.getInstrumentId(), OrderDirection.ORDER_DIRECTION_BUY, quantity, price);
    log.info("Вход по стратегии: {} по цене: {} (лотов: {})", instrument.getInstrumentId(), price, quantity);
  }

  @Override
  public void onStrategyExitAction(CandleInstrument instrument, Bar bar) {
    String instrumentId = instrument.getInstrumentId();
    var closePrice = bar.getClosePrice().bigDecimalValue();
    cancelOpenedOrdersForInstrument(instrumentId);
    long quantity = getMaxSellLots(instrumentId);
    var price = getLotPrice(instrumentId, closePrice, OrderDirection.ORDER_DIRECTION_SELL);
    if (quantity <= 0) {
      throw new IllegalStateException("Недостаточно лотов для открытия сделки");
    }
    postLimitOrder(instrument.getInstrumentId(), OrderDirection.ORDER_DIRECTION_SELL, quantity, price);
    log.info("Выход по стратегии: {} по цене: {} (лотов: {})", instrument.getInstrumentId(), price, quantity);
  }

  private Function<BarSeries, Strategy> createStrategy(int shortEmaPeriod, int longEmaPeriod) {
    return barSeries -> {
      ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
      EMAIndicator shortEma = new EMAIndicator(closePrice, shortEmaPeriod);
      EMAIndicator longEma = new EMAIndicator(closePrice, longEmaPeriod);
      Rule buyingRule = new CrossedUpIndicatorRule(shortEma, longEma);
      Rule sellingRule = new CrossedDownIndicatorRule(shortEma, longEma);
      return new BaseStrategy(buyingRule, sellingRule);
    };
  }

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

  private long getMaxBuyLots(String instrumentId, BigDecimal price) {
    var getMaxLotsRequest = GetMaxLotsRequest.newBuilder()
      .setAccountId(tradingAccountId)
      .setInstrumentId(instrumentId)
      .setPrice(NumberMapper.bigDecimalToQuotation(price))
      .build();
    var response = ordersService.callSyncMethod(stub -> stub.getMaxLots(getMaxLotsRequest));
    return response.getBuyLimits().getBuyMaxLots();
  }

  private long getMaxSellLots(String instrumentId) {
    var getMaxLotsRequest = GetMaxLotsRequest.newBuilder()
      .setAccountId(tradingAccountId)
      .setInstrumentId(instrumentId)
      .build();
    var response = ordersService.callSyncMethod(stub -> stub.getMaxLots(getMaxLotsRequest));
    return response.getSellLimits().getSellMaxLots();
  }

  private BigDecimal getLotPrice(String instrumentId, BigDecimal price, OrderDirection direction) {
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

  private BigDecimal getMinPriceIncrement(String instrumentId) {
    var instrumentRequest = InstrumentRequest.newBuilder()
      .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_UID)
      .setId(instrumentId)
      .build();
    var instrumentResponse = instrumentsService.callSyncMethod(stub -> stub.getInstrumentBy(instrumentRequest));
    return NumberMapper.quotationToBigDecimal(instrumentResponse.getInstrument().getMinPriceIncrement());
  }

  private BigDecimal roundUpPrice(BigDecimal price, BigDecimal minPriceIncrement) {
    return price.divide(minPriceIncrement, 0, RoundingMode.UP).multiply(minPriceIncrement);
  }

  private BigDecimal roundDownPrice(BigDecimal price, BigDecimal minPriceIncrement) {
    return price.divide(minPriceIncrement, 0, RoundingMode.DOWN).multiply(minPriceIncrement);
  }

  private void payInSandbox() {
    var balanceRequest = WithdrawLimitsRequest.newBuilder().setAccountId(tradingAccountId).build();
    var balanceResponse = sandboxService.callSyncMethod(stub -> stub.getSandboxWithdrawLimits(balanceRequest));
    var balance = balanceResponse.getMoneyList().stream().filter(moneyValue -> moneyValue.getCurrency().equals("rub"))
      .findFirst()
      .map(NumberMapper::moneyValueToBigDecimal)
      .orElse(BigDecimal.ZERO);
    var configBalance = BigDecimal.valueOf(properties.getBalance());
    log.info("Баланс: {} (настройка: {})", balance, configBalance);
    if (balance.compareTo(BigDecimal.valueOf(properties.getBalance())) < 0) {
      var amount = configBalance.subtract(balance);
      var payInRequest = SandboxPayInRequest.newBuilder()
        .setAccountId(tradingAccountId)
        .setAmount(NumberMapper.bigDecimalToMoneyValue(amount, "rub"))
        .build();
      sandboxService.callSyncMethod(stub -> stub.sandboxPayIn(payInRequest));
      log.info("Баланс песочницы пополнен на сумму: {} руб.", amount);
    }
  }
}
