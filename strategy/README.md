# Модуль java-sdk-strategy

Предоставляет конструктор для бэктеста и лайвтрейдинга стратегий на основе японских свечей.

## Решаемые задачи:

* Бэктест стратегий на основе японских свечей по конкретному инструменту
* Поиск на бэктесте стратегии с наиболее профитными параметрами
* Запуск лайвтрейдинга стратегий на инструментах и выполнение заданных действий при входе или выходе по стратегии

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
    ...
</dependencies>
```

</details>
<details>
<summary>Gradle</summary>

```groovy
implementation 'ru.tinkoff.piapi:java-sdk-core:1.30'
implementation 'ru.tinkoff.piapi:java-sdk-strategy:1.30'
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
