# Модуль java-sdk-storage-jdbc

Содержит в себе имплементации репозиториев для хранения рыночных данных в БД.

На данный момент полностью поддерживаются следующие СУБД:

* [PostgreSQL](https://www.postgresql.org/)
* [MySQL](https://www.mysql.com/)

## Добавление модуля в проект

<details>
<summary>Maven</summary>

```xml

<dependencies>
    ...
    <dependency>
        <groupId>ru.tinkoff.piapi</groupId>
        <artifactId>java-sdk-storage-jdbc</artifactId>
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
implementation 'ru.tinkoff.piapi:java-sdk-storage-jdbc:1.30'
```

</details>

Также для работы с модулем требуется добавить в зависимости jdbc драйвер вашей СУБД:

* PostgreSQL:

    <details>
    <summary>Maven</summary>

    ```xml
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.7.5</version>
    </dependency>
    ```
    </details>

    <details>
    <summary>Gradle</summary>

    ```groovy
    implementation 'org.postgresql:postgresql:42.7.5'
    ```
    </details>

* MySQL:
    <details>
    <summary>Maven</summary>

    ```xml
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>9.2.0</version>
    </dependency>
    ```
    </details>
    <details>
    <summary>Gradle</summary>

    ```groovy
    implementation 'com.mysql:mysql-connector-j:9.2.0'
    ```
    </details>

## Пример использования

<details>
<summary>Пример сохранения закрытых минутных свечей акции Т-Технологии в СУБД PostgreSQL</summary>

```java
public class Main {

    public static void main(String[] args) {
        var connectorConfiguration = ConnectorConfiguration.loadFromPropertiesFile("invest.properties");
        var unaryServiceFactory = ServiceStubFactory.create(connectorConfiguration);
        var streamServiceFactory = StreamServiceStubFactory.create(unaryServiceFactory);
        var streamManagerFactory = StreamManagerFactory.create(streamServiceFactory);
        var executorService = Executors.newCachedThreadPool();
        var scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        var jdbcConfiguration = new JdbcConfiguration(createDataSource(), "trading", "candles");
        var candlesRepository = new CandlesJdbcRepository(jdbcConfiguration);
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
    }

    private static DataSource createDataSource() {
        var pgDataSource = new PGSimpleDataSource();
        pgDataSource.setUrl("jdbc:postgresql://localhost:5432/invest");
        pgDataSource.setUser("user");
        pgDataSource.setPassword("password");
        return pgDataSource;
    }
}
```

</details>

Поднять СУБД локально можно с помощью [docker](https://www.docker.com/), выполнив команду:

```bash
docker run --name postgres -e POSTGRES_USER=user -e POSTGRES_PASSWORD=password -e POSTGRES_DB=invest -p 5432:5432 postgres:16
```
