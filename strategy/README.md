# Модуль java-sdk-strategy
Предоставляет конструктор для бэктеста и лайвтрейдинга стратегий на основе японских свечей.

## Решаемые задачи:
* Бэктест стратегий на основе японских свечей по конкретному инструменту
* Поиск на бэктесте стратегии с наиболее профитными параметрами
* Запуск лайвтрейдинга стратегий на инструментах и выполнение заданных действий при входе или выходе по стратегии

## Лайвтрейдинг
Конструктор принимает на вход таблицу инструментов и стратегий.
В зависимости от рыночных данных и вычислений библиотеки ta4j в листенеры приходят сообщения
о сигналах на вход или выход по стратегии соответствующего инструмента.
<br>Пример: [LiveStrategy](../example/basic-example/src/main/java/ru/ttech/piapi/example/strategy/live/LiveCandleStrategyExample.java).

## Бэктест
Может протестировать несколько стратегий на одном инструменте. Свечи загружаются из zip-архива
и собираются в указанный пользователем таймфрейм
<br>Пример: [BacktestStrategy](../example/basic-example/src/main/java/ru/ttech/piapi/example/strategy/backtest/BacktestExample.java).

Также используя инструменты библиотеки ta4j можно выбрать стратегию с наибольшей прибыльностью за определённый
период на конкретном инструменте.
<br>Пример: [ChooseBestStrategy](../example/basic-example/src/main/java/ru/ttech/piapi/example/strategy/backtest/ChooseBestStrategyExample.java).
