package ru.ttech.piapi.springboot.example.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.GetCandlesRequest;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.springboot.bot.TradingBot;

@Slf4j
@Component
public class CandleTradingBot implements TradingBot {

  @Override
  public CandleInstrument getInstrument() {
    return CandleInstrument.newBuilder()
      .setInstrumentId("87db07bc-0e02-4e29-90bb-05e8ef791d7b")
      .setInterval(SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE)
      .build();
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
  public Strategy getStrategy(BarSeries barSeries) {
    ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
    SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
    SMAIndicator longSma = new SMAIndicator(closePrice, 30);
    Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma);
    Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma);
    return new BaseStrategy(buyingRule, sellingRule);
  }

  @Override
  public void onStrategyEnterAction(Bar bar) {
    log.info("Entering position by price: {}", bar.getClosePrice());
  }

  @Override
  public void onStrategyExitAction(Bar bar) {
    log.info("Exiting position by price: {}", bar.getClosePrice());
  }
}
