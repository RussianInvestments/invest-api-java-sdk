[![Version](https://img.shields.io/maven-central/v/ru.tinkoff.piapi/java-sdk?logo=apache-maven&style=flat-square)](https://search.maven.org/artifact/ru.tinkoff.piapi/java-sdk)
[![Release](https://jitpack.io/v/RussianInvestments/invest-api-java-sdk.svg?style=flat-square)](https://jitpack.io/#RussianInvestments/invest-api-java-sdk)
[![License](https://img.shields.io/github/license/RussianInvestments/invest-api-java-sdk?style=flat-square&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
[![GitHub Actions Status](<https://img.shields.io/github/workflow/status/RussianInvestments/invest-api-java-sdk/Java CI with Maven?logo=GitHub&style=flat-square>)](https://github.com/RussianInvestments/invest-api-java-sdk/actions?query=workflow%3A"Java+CI+with+Maven")

# Java SDK для T-Invest API

Данный проект представляет собой инструментарий на языке Java для работы с API Т-Инвестиции, который можно
использовать для создания торговых роботов.

## Требования

- Java версии не ниже 11


## Использование

Для начала работы подключите к вашему проекту core-модуль

|     Система сборки     | Код                                                                                                                                                                                                                                                                                                                   |
|:----------------------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|         Maven          | <b>\<dependency></b><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>\<groupId></b>ru.tinkoff.piapi<b>\</groupId></b><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>\<artifactId></b>java-sdk-core<b>\</artifactId></b><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>\<version></b>1.27<b>\</version></b><br><b>\</dependency></b> |
 | Gradle with Groovy DSL | <b>implementation</b> 'ru.tinkoff.piapi:java-sdk-core:1.27'                                                                                                                                                                                                                                                           |
 | Gradle with Kotlin DSL | <b>implementation</b>("ru.tinkoff.piapi:java-sdk-core:1.27")                                                                                                                                                                                                                                                          |



После этого можно пользоваться инструментарием

```java
import ru.tinkoff.piapi.core.InvestApi;

var token = "<secret-token>";
var api = InvestApi.create(token);

var order = api.getOrdersService().postOrderSync(...)
```

## Для разработчиков

В main ветке snapshot версия следующего (разрабатываемого) релиза.
Последний стабильный релиз отмечается тэгом с соответствующим номером релиза.
Для внесения изменения и предложений по sdk сделайте fork репозитария, создайте ветку от main, внесите доработки и создайте pull request в этот репозиторий.


### Сборка

#### JVM
Шаги для сборки:
1) скачать репозиторий ```git pull https://github.com/RussianInvestments/invest-api-java-sdk.git```
2) перейдите в директорию проекта командой ```cd invest-api-java-sdk```
3) для сборки перейдите в директорию проекта и соберите проект командой ```mvn clean package```

#### Native

Для сборки native образа потребуется добавить зависимость от `native-image-support` в свой проект:

Maven:
```xml
<dependency>
 <groupId>com.google.cloud</groupId>
 <artifactId>native-image-support</artifactId>
 <version>0.14.1</version>
</dependency>
```
Gradle:
```groovy
implementation 'com.google.cloud:native-image-support:0.14.1'
```

К аргументам сборки [GraalVM](https://github.com/oracle/graal/blob/master/docs/reference-manual/native-image/README.md) необходимо добавить:
```
--initialize-at-build-time=ch.qos.logback,org.slf4j.LoggerFactory,org.slf4j.simple.SimpleLogger,org.slf4j.impl.StaticLoggerBinder,org.slf4j.MDC
```

## Предложения и пожелания к SDK

Смело выносите свои предложения в Issues, задавайте вопросы. Pull Request'ы также принимаются.

## У меня есть вопрос по работе API

Документация к API находится в [отдельном репозитории](https://RussianInvestments.github.io/investAPI). Там вы можете задать
вопрос в Issues.
