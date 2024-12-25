package ru.tinkoff.piapi.example;

import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.GetTechAnalysisRequest;
import ru.tinkoff.piapi.contract.v1.GetTechAnalysisResponse;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.core.InvestApi;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

public class TechAnalysisExample {

  private static final String TCS_UID = "87db07bc-0e02-4e29-90bb-05e8ef791d7b"; // UID акции Т-Технологии на Московской бирже
  private static final Logger log = LoggerFactory.getLogger(TechAnalysisExample.class);
  private static final Executor delayedExecutor = CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS);

  public static void main(String[] args) {
    String token = args[0];
    var api = InvestApi.create(token);

    techAnalysisServiceExample(api);

    CompletableFuture.runAsync(() -> log.info("starting shutdown"), delayedExecutor)
      .thenAcceptAsync(__ -> api.destroy(3), delayedExecutor)
      .join();
  }

  private static void techAnalysisServiceExample(InvestApi api) {
    // Начало периода -- на 30 минут раньше от текущего времени по UTC
    var timeFrom = Timestamp.newBuilder()
      .setSeconds(Instant.now().minus(1, ChronoUnit.HOURS).getEpochSecond())
      .build();
    // Конец периода -- текущее время по UTC
    var timeTo = Timestamp.newBuilder()
      .setSeconds(Instant.now().getEpochSecond())
      .build();

    // Выполняем запросы для получения данных различных индикаторов
    smaRequestExample(api, timeFrom, timeTo);
    emaRequestExample(api, timeFrom, timeTo);
    bbRequestExample(api, timeFrom, timeTo);
    rsiRequestExample(api, timeFrom, timeTo);
    macdRequestExample(api, timeFrom, timeTo);
  }

  private static void smaRequestExample(InvestApi api, Timestamp timeFrom, Timestamp timeTo) {
    // Собираем запрос для SMA индикатора
    GetTechAnalysisRequest request = GetTechAnalysisRequest.newBuilder()
      .setIndicatorType(GetTechAnalysisRequest.IndicatorType.INDICATOR_TYPE_SMA) // Индикатор SMA
      .setInstrumentUid(TCS_UID) // UID акции Т-Технологий
      .setFrom(timeFrom) // Начало запрашиваемого периода
      .setTo(timeTo) // Конец запрашиваемого периода
      .setInterval(GetTechAnalysisRequest.IndicatorInterval.INDICATOR_INTERVAL_FIFTEEN_MINUTES) // Свеча 15 минут
      .setTypeOfPrice(GetTechAnalysisRequest.TypeOfPrice.TYPE_OF_PRICE_CLOSE) // Считаем по цене закрытия
      .setLength(9) // Период SMA
      .build();
    executeRequest(api, request);
  }

  private static void emaRequestExample(InvestApi api, Timestamp timeFrom, Timestamp timeTo) {
    // Собираем запрос для EMA индикатора
    GetTechAnalysisRequest request = GetTechAnalysisRequest.newBuilder()
      .setIndicatorType(GetTechAnalysisRequest.IndicatorType.INDICATOR_TYPE_EMA) // Индикатор EMA
      .setInstrumentUid(TCS_UID) // UID акции Т-Технологий
      .setFrom(timeFrom) // Начало запрашиваемого периода
      .setTo(timeTo) // Конец запрашиваемого периода
      .setInterval(GetTechAnalysisRequest.IndicatorInterval.INDICATOR_INTERVAL_FIFTEEN_MINUTES) // Свеча 15 минут
      .setTypeOfPrice(GetTechAnalysisRequest.TypeOfPrice.TYPE_OF_PRICE_CLOSE) // Считаем по цене закрытия
      .setLength(9) // Период EMA
      .build();
    executeRequest(api, request);
  }

  private static void bbRequestExample(InvestApi api, Timestamp timeFrom, Timestamp timeTo) {
    // Собираем запрос для индикатора линий Боллинджера
    GetTechAnalysisRequest request = GetTechAnalysisRequest.newBuilder()
      .setIndicatorType(GetTechAnalysisRequest.IndicatorType.INDICATOR_TYPE_BB) // Индикатор Bollinger Bands
      .setInstrumentUid(TCS_UID) // UID акции Т-Технологий
      .setFrom(timeFrom) // Начало запрашиваемого периода
      .setTo(timeTo) // Конец запрашиваемого периода
      .setInterval(GetTechAnalysisRequest.IndicatorInterval.INDICATOR_INTERVAL_FIFTEEN_MINUTES) // Свеча 15 минут
      .setTypeOfPrice(GetTechAnalysisRequest.TypeOfPrice.TYPE_OF_PRICE_CLOSE) // Считаем по цене закрытия
      .setLength(20)
      .setDeviation(GetTechAnalysisRequest.Deviation.newBuilder()
        .setDeviationMultiplier(Quotation.newBuilder().setUnits(2).build())
        .build()) // Стандартное отклонение
      .build();
    executeRequest(api, request);
  }

  private static void rsiRequestExample(InvestApi api, Timestamp timeFrom, Timestamp timeTo) {
    // Собираем запрос для индикатора RSI
    GetTechAnalysisRequest request = GetTechAnalysisRequest.newBuilder()
      .setIndicatorType(GetTechAnalysisRequest.IndicatorType.INDICATOR_TYPE_RSI) // Индикатор Bollinger Bands
      .setInstrumentUid(TCS_UID) // UID акции Т-Технологий
      .setFrom(timeFrom) // Начало запрашиваемого периода
      .setTo(timeTo) // Конец запрашиваемого периода
      .setInterval(GetTechAnalysisRequest.IndicatorInterval.INDICATOR_INTERVAL_FIFTEEN_MINUTES) // Свеча 15 минут
      .setTypeOfPrice(GetTechAnalysisRequest.TypeOfPrice.TYPE_OF_PRICE_CLOSE) // Считаем по цене закрытия
      .setLength(14) // период RSI
      .build();
    executeRequest(api, request);
  }

  private static void macdRequestExample(InvestApi api, Timestamp timeFrom, Timestamp timeTo) {
    // Собираем запрос для индикатора MACD
    GetTechAnalysisRequest request = GetTechAnalysisRequest.newBuilder()
      .setIndicatorType(GetTechAnalysisRequest.IndicatorType.INDICATOR_TYPE_MACD) // Индикатор MACD
      .setInstrumentUid(TCS_UID) // UID акции Т-Технологий
      .setFrom(timeFrom) // Начало запрашиваемого периода
      .setTo(timeTo) // Конец запрашиваемого периода
      .setInterval(GetTechAnalysisRequest.IndicatorInterval.INDICATOR_INTERVAL_FIFTEEN_MINUTES) // Свеча 15 минут
      .setTypeOfPrice(GetTechAnalysisRequest.TypeOfPrice.TYPE_OF_PRICE_CLOSE) // Считаем по цене закрытия
      .setSmoothing(GetTechAnalysisRequest.Smoothing.newBuilder()
        .setFastLength(12)
        .setSlowLength(26)
        .setSignalSmoothing(9)
        .build()) // Параметры сглаживания
      .build();
    executeRequest(api, request);
  }

  private static void executeRequest(InvestApi api, GetTechAnalysisRequest request) {
    // Получаем ответ синхронно
    var techAnalysisResponse = api.getMarketDataService().getTechAnalysisSync(request);
    log.info("Синхронное получение данных индикатора {} по акции Т-Технологии:", request.getIndicatorType().name());
    techAnalysisResponse.getTechnicalIndicatorsList()
      .forEach(item -> printTechAnalysisItem(request.getIndicatorType(), item));

    // Получаем ответ асинхронно
    api.getMarketDataService().getTechAnalysis(request)
      .thenAccept(response -> {
        log.info("Асинхронное получение данных индикатора {} по акции Т-Технологии:", request.getIndicatorType().name());
        response.getTechnicalIndicatorsList()
          .forEach(item -> printTechAnalysisItem(request.getIndicatorType(), item));
      })
      .exceptionally(__ -> {
        log.error("Произошла ошибка при получении данных индикатора");
        return null;
      });
  }

  private static void printTechAnalysisItem(
    GetTechAnalysisRequest.IndicatorType indicatorType,
    GetTechAnalysisResponse.TechAnalysisItem techAnalysisItem
  ) {
    switch (indicatorType) {
      case INDICATOR_TYPE_EMA:
      case INDICATOR_TYPE_SMA:
      case INDICATOR_TYPE_RSI:
        printTechAnalysisSignalItem(techAnalysisItem);
        break;
      case INDICATOR_TYPE_BB:
        printTechAnalysisBollingerItem(techAnalysisItem);
        break;
      case INDICATOR_TYPE_MACD:
        printTechAnalysisMACDItem(techAnalysisItem);
    }
  }

  private static void printTechAnalysisSignalItem(GetTechAnalysisResponse.TechAnalysisItem techAnalysisItem) {
    log.info(
      "Время {}, Значение индикатора: {}",
      Instant.ofEpochSecond(techAnalysisItem.getTimestamp().getSeconds()),
      quotationToBigDecimal(techAnalysisItem.getSignal())
    );
  }

  private static void printTechAnalysisBollingerItem(GetTechAnalysisResponse.TechAnalysisItem techAnalysisItem) {
    log.info(
      "Время {}, Нижняя линия Боллинджера: {}, Средняя линия Боллинджера: {}, Верхняя линия Боллинджера: {} ",
      Instant.ofEpochSecond(techAnalysisItem.getTimestamp().getSeconds()),
      quotationToBigDecimal(techAnalysisItem.getLowerBand()),
      quotationToBigDecimal(techAnalysisItem.getMiddleBand()),
      quotationToBigDecimal(techAnalysisItem.getUpperBand())
    );
  }

  private static void printTechAnalysisMACDItem(GetTechAnalysisResponse.TechAnalysisItem techAnalysisItem) {
    log.info(
      "Время {}, линия MACD: {}, сигнальная линия: {} ",
      Instant.ofEpochSecond(techAnalysisItem.getTimestamp().getSeconds()),
      quotationToBigDecimal(techAnalysisItem.getMacd()),
      quotationToBigDecimal(techAnalysisItem.getSignal())
    );
  }
}
