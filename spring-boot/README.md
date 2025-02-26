# Модуль java-sdk-spring-boot-starter

Стартер для интеграции SDK в приложение на Spring Boot

## Решаемые задачи:

* Быстрое создание торгового робота
* Автоматическая конфигурация подключения к API
* Загрузка исторических данных для стратегии

## Добавление модуля в проект

<details>
<summary>Maven</summary>

```xml

<dependencies>
    ...
    <dependency>
        <groupId>ru.tinkoff.piapi</groupId>
        <artifactId>java-sdk-core</artifactId>
        <version>1.30</version>
    </dependency>
    <dependency>
        <groupId>ru.tinkoff.piapi</groupId>
        <artifactId>java-sdk-strategy</artifactId>
        <version>1.30</version>
    </dependency>
    <dependency>
        <groupId>ru.tinkoff.piapi</groupId>
        <artifactId>java-sdk-spring-boot-starter</artifactId>
        <version>1.30</version>
    </dependency>
    ...
</dependencies>
```

</details>
<details>
<summary>Gradle</summary>

```groovy
implementation 'ru.tinkoff.piapi:java-sdk-core:1.30'
implementation 'ru.tinkoff.piapi:java-sdk-strategy:1.30'
implementation 'ru.tinkoff.piapi:java-sdk-spring-boot-starter:1.30'
```

</details>

## Использование

Для начала необходимо в `application.yml` указать токен для подключения к API Т-Инвестиций:

```yaml
invest:
  connector:
    token: ${INVEST_TOKEN}
```

Теперь можно создать бота для торговли по стратегии на основе японских свечей:

```java

@Component
public class MyCandleTradingBot implements CandleTradingBot {

    @Override
    public GetCandlesRequest.CandleSource getCandleSource() {
        // источник свечных данных
        return GetCandlesRequest.CandleSource.CANDLE_SOURCE_INCLUDE_WEEKEND;
    }

    @Override
    public int getWarmupLength() {
        // количество свечей, которые будут загружены для стабилизации значений индикаторов
        // лучше ставить это значения равным самому большому периоду индикатора, используемого в Вашей стратегии
        return 100;
    }

    @Override
    public Map<CandleInstrument, Function<BarSeries, Strategy>> setStrategies() {
        // настраиваем бота на торговлю акцией Т-Технологии по стратегии ta4j
        var ttechShare = CandleInstrument.newBuilder()
                .setInstrumentId("87db07bc-0e02-4e29-90bb-05e8ef791d7b")
                .setInterval(SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE)
                .build();
        return Map.of(
                ttechShare, createStrategy(5, 10)
        );
    }

    @Override
    public void onStrategyEnterAction(CandleInstrument instrument, Bar bar) {
        // выполняем действие при входе в позицию
        log.info("Entering position for instrument {} by price: {}", instrument.getInstrumentId(), bar.getClosePrice());
    }

    @Override
    public void onStrategyExitAction(CandleInstrument instrument, Bar bar) {
        // выполняем действие при выходе из позиции
        log.info("Exiting position for instrument {} by price: {}", instrument.getInstrumentId(), bar.getClosePrice());
    }

    // Задаём стратегию на двух индикаторах EMA
    public Function<BarSeries, Strategy> createStrategy(int shortEmaPeriod, int longEmaPeriod) {
        return barSeries -> {
            ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
            EMAIndicator shortEma = new EMAIndicator(closePrice, shortEmaPeriod);
            EMAIndicator longEma = new EMAIndicator(closePrice, longEmaPeriod);
            Rule buyingRule = new CrossedUpIndicatorRule(shortEma, longEma);
            Rule sellingRule = new CrossedDownIndicatorRule(shortEma, longEma);
            return new BaseStrategy(buyingRule, sellingRule);
        };
    }
}
```

Для создания торгового робота на основе японских свечей Вам нужно реализовать интерфейс CandleTradingBot.
Дальше автоконфигурация загрузит необходимые данные, подключится к MarketDataStream и повесит на него листенеры.
Вам остаётся написать свою логику в методах `onStrategyEnterAction` и `onStrategyExitAction`
