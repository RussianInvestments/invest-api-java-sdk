# Модуль java-sdk-strategy

Предоставляет конструктор для бэктеста и лайвтрейдинга стратегий на основе японских свечей.

## Решаемые задачи:

* Бэктест стратегий на основе японских свечей по конкретному инструменту
* Поиск на бэктесте стратегии с наиболее профитными параметрами
* Запуск лайвтрейдинга стратегий на инструментах и выполнение заданных действий при входе или выходе по стратегии
* Загрузка архивных данных для последующей обработки

## Добавление модуля в проект

<details>
<summary>Maven</summary>

```xml

<dependencies>
    ...
    <dependency>
        <groupId>ru.tinkoff.piapi</groupId>
        <artifactId>java-sdk-core</artifactId>
        <version>1.31</version>
    </dependency>
    <dependency>
        <groupId>ru.tinkoff.piapi</groupId>
        <artifactId>java-sdk-strategy</artifactId>
        <version>1.31</version>
    </dependency>
    ...
</dependencies>
```

</details>
<details>
<summary>Gradle</summary>

```groovy
implementation 'ru.tinkoff.piapi:java-sdk-core:1.31'
implementation 'ru.tinkoff.piapi:java-sdk-strategy:1.31'
```

</details>

## Лайвтрейдинг

Конструктор принимает на вход таблицу инструментов и стратегий.
В зависимости от рыночных данных и вычислений библиотеки ta4j в листенеры приходят сообщения
о сигналах на вход или выход по стратегии соответствующего инструмента.
<br>
Пример: [LiveStrategy](../example/basic-example/src/main/java/ru/ttech/piapi/example/strategy/live/LiveCandleStrategyExample.java).
</br>

## Бэктест

Может протестировать несколько стратегий на одном инструменте. Свечи загружаются из zip-архива
и собираются в указанный пользователем таймфрейм
<br>
Пример: [BacktestStrategy](../example/basic-example/src/main/java/ru/ttech/piapi/example/strategy/backtest/BacktestExample.java).
</br>

Также используя инструменты библиотеки ta4j можно выбрать стратегию с наибольшей прибыльностью за определённый
период на конкретном инструменте.
<br>
Пример: [ChooseBestStrategy](../example/basic-example/src/main/java/ru/ttech/piapi/example/strategy/backtest/ChooseBestStrategyExample.java).
</br>

## Загрузка архивных данных

Вы можете загрузить архивные данные свечей по любому инструменту. По каждому инструменту на выходе получится один csv-файл в формате:
```csv
start_time,open,high,low,close,volume
2018-03-07T18:51:00Z,2263.0,2263.0,2263.0,2263.0,4
...
```
<details>
<summary>Пример загрузки архивных свечных данных</summary>

```java
public class Main {
  public static void main(String[] args) {
    var configuration = ConnectorConfiguration.loadFromPropertiesFile("invest.properties");
    var unaryServiceFactory = ServiceStubFactory.create(configuration);
    var executorService = Executors.newCachedThreadPool();
    var barsLoader = new BarsLoader(null, configuration, executorService);
    var instrumentsService = unaryServiceFactory.newSyncService(InstrumentsServiceGrpc::newBlockingStub);
    // получаем список всех акций
    var response = instrumentsService.callSyncMethod(stub -> stub.shares(InstrumentsRequest.getDefaultInstance()));
    // фильтруем по доступности, загружаем архивы минутных свечей и формируем файл с агрегированными свечами
    response.getInstrumentsList().stream()
      .filter(share -> share.getTradingStatus() == SecurityTradingStatus.SECURITY_TRADING_STATUS_DEALER_NORMAL_TRADING
        && share.getApiTradeAvailableFlag())
      .forEach(share -> {
        LocalDate from = TimeMapper.timestampToLocalDateTime(share.getFirst1MinCandleDate()).toLocalDate();
        String instrumentId = share.getUid();
        CandleInterval interval = CandleInterval.CANDLE_INTERVAL_1_MIN; // заменить на требуемый
        String filename = String.format("%s_%s.csv", instrumentId, interval).toLowerCase();
        var bars = barsLoader.loadBars(instrumentId, interval, from);
        barsLoader.saveBars(Path.of(filename), bars);
      });
    executorService.shutdown();
  }
}
```
</details>
