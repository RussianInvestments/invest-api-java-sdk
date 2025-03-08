# Модуль java-sdk-storage-csv

Содержит в себе имплементации репозиториев для хранения рыночных данных в csv-файлах.

## Добавление модуля в проект

<details>
<summary>Maven</summary>

```xml

<dependencies>
    ...
    <dependency>
        <groupId>ru.tinkoff.piapi</groupId>
        <artifactId>java-sdk-storage-csv</artifactId>
        <version>1.30</version>
    </dependency>
    <dependency>
        <groupId>ru.tinkoff.piapi</groupId>
        <artifactId>java-sdk-strategy</artifactId>
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
implementation 'ru.tinkoff.piapi:java-sdk-storage-csv:1.30'
```

</details>

## Пример использования

<details>
<summary>Пример сохранения закрытых минутных свечей акции Т-Технологии в CSV-файл</summary>

```java
public class Main {

    public static void main(String[] args) {
        var connectorConfiguration = ConnectorConfiguration.loadFromPropertiesFile("invest.properties");
        var unaryServiceFactory = ServiceStubFactory.create(connectorConfiguration);
        var streamServiceFactory = StreamServiceStubFactory.create(unaryServiceFactory);
        var streamManagerFactory = StreamManagerFactory.create(streamServiceFactory);
        var executorService = Executors.newCachedThreadPool();
        var scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        CsvConfiguration csvConfiguration = new CsvConfiguration(Path.of("candles.csv"));
        try (var candlesRepository = new CandlesCsvRepository(csvConfiguration)) {
            var marketDataStreamManager = streamManagerFactory.newMarketDataStreamManager(executorService, scheduledExecutorService);
            marketDataStreamManager.subscribeCandles(Set.of(
                            new Instrument(
                                    "87db07bc-0e02-4e29-90bb-05e8ef791d7b",
                                    SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE
                            )
                    ),
                    new CandleSubscriptionSpec(),
                    candle -> candlesRepository.save(candle.getOriginal())
            );
            marketDataStreamManager.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
```

</details>
