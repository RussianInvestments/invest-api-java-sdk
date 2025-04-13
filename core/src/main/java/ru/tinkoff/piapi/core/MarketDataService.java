package ru.tinkoff.piapi.core;

import ru.tinkoff.piapi.core.utils.DateUtils;
import ru.tinkoff.piapi.core.utils.Helpers;
import ru.tinkoff.piapi.core.utils.ValidationUtils;
import ru.tinkoff.piapi.contract.v1.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static ru.tinkoff.piapi.contract.v1.MarketDataServiceGrpc.MarketDataServiceBlockingStub;
import static ru.tinkoff.piapi.contract.v1.MarketDataServiceGrpc.MarketDataServiceStub;

@Deprecated(since = "1.30", forRemoval = true)
public class MarketDataService {
  private final MarketDataServiceBlockingStub marketDataBlockingStub;
  private final MarketDataServiceStub marketDataStub;

  MarketDataService(@Nonnull MarketDataServiceBlockingStub marketDataBlockingStub,
                    @Nonnull MarketDataServiceStub marketDataStub) {
    this.marketDataBlockingStub = marketDataBlockingStub;
    this.marketDataStub = marketDataStub;
  }

  /**
   * Получение (синхронное) списка обезличенных сделок по инструменту.
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @param from         Начало периода (по UTC).
   * @param to           Окончание периода (по UTC).
   * @return Список обезличенных сделок по инструменту.
   */
  @Nonnull
  public List<Trade> getLastTradesSync(@Nonnull String instrumentId,
                                       @Nonnull Instant from,
                                       @Nonnull Instant to) {
    ValidationUtils.checkFromTo(from, to);

    return Helpers.unaryCall(() -> marketDataBlockingStub.getLastTrades(
        GetLastTradesRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .build())
      .getTradesList());
  }

  /**
   * Получение (синхронное) списка обезличенных сделок по инструменту за последний час.
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @return Список обезличенных сделок по инструменту.
   */
  @Nonnull
  public List<Trade> getLastTradesSync(@Nonnull String instrumentId) {
    var to = Instant.now();
    var from = to.minus(60, ChronoUnit.MINUTES);
    return getLastTradesSync(instrumentId, from, to);
  }

  /**
   * Получение (синхронное) списка свечей по инструменту.
   *
   * @param instrumentId идентификатор инструмента. Может принимать значение FIGI или uid
   * @param from         Начало периода (по UTC).
   * @param to           Окончание периода (по UTC).
   * @param interval     Интервал свечей
   * @return Список свечей
   */
  @Nonnull
  public List<HistoricCandle> getCandlesSync(@Nonnull String instrumentId,
                                             @Nonnull Instant from,
                                             @Nonnull Instant to,
                                             @Nonnull CandleInterval interval) {
    return getCandlesSync(instrumentId, from, to, interval, GetCandlesRequest.CandleSource.CANDLE_SOURCE_UNSPECIFIED);
  }

  /**
   * Получение (синхронное) списка свечей по инструменту.
   *
   * @param instrumentId идентификатор инструмента. Может принимать значение FIGI или uid
   * @param from         Начало периода (по UTC).
   * @param to           Окончание периода (по UTC).
   * @param interval     Интервал свечей
   * @param source       Источник свечи (только биржевые или все свечи)
   * @return Список свечей
   */
  @Nonnull
  public List<HistoricCandle> getCandlesSync(@Nonnull String instrumentId,
                                             @Nonnull Instant from,
                                             @Nonnull Instant to,
                                             @Nonnull CandleInterval interval,
                                             @Nonnull GetCandlesRequest.CandleSource source) {
    ValidationUtils.checkFromTo(from, to);

    return Helpers.unaryCall(() -> marketDataBlockingStub.getCandles(
        GetCandlesRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .setInterval(interval)
          .setCandleSourceType(source)
          .build())
      .getCandlesList());
  }

  /**
   * Получение (синхронное) списка последних цен по инструментам
   *
   * @param instrumentIds FIGI-идентификатор / uid инструмента.
   * @return Список последний цен
   */
  @Nonnull
  public List<LastPrice> getLastPricesSync(@Nonnull Iterable<String> instrumentIds) {
    return Helpers.unaryCall(() -> marketDataBlockingStub.getLastPrices(
        GetLastPricesRequest.newBuilder()
          .addAllInstrumentId(instrumentIds)
          .build())
      .getLastPricesList());
  }

  /**
   * Получение (синхронное) списка последних цен по инструментам
   *
   * @param instrumentIds FIGI-идентификатор / uid инструмента.
   * @param lastPriceType Тип запрашиваемой последней цены.
   * @return Список последний цен
   */
  @Nonnull
  public List<LastPrice> getLastPricesSync(@Nonnull Iterable<String> instrumentIds,
                                           @Nullable LastPriceType lastPriceType) {
    GetLastPricesRequest.Builder request = GetLastPricesRequest.newBuilder()
        .addAllInstrumentId(instrumentIds);
    if (lastPriceType != null) {
      request.setLastPriceType(lastPriceType);
    }
    return Helpers.unaryCall(() -> marketDataBlockingStub.getLastPrices(request.build())
      .getLastPricesList());
  }

  /**
   * Получение (синхронное) информации о стакане
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @param depth        глубина стакана. Может принимать значения 1, 10, 20, 30, 40, 50
   * @return стакан для инструмента
   */
  @Nonnull
  public GetOrderBookResponse getOrderBookSync(@Nonnull String instrumentId, int depth) {
    return Helpers.unaryCall(() -> marketDataBlockingStub.getOrderBook(
      GetOrderBookRequest.newBuilder()
        .setInstrumentId(instrumentId)
        .setDepth(depth)
        .build()));
  }

  /**
   * Получение (синхронное) текущего торгового статуса инструмента
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @return текущий торговый статус инструмента
   */
  @Nonnull
  public GetTradingStatusResponse getTradingStatusSync(@Nonnull String instrumentId) {
    return Helpers.unaryCall(() -> marketDataBlockingStub.getTradingStatus(
      GetTradingStatusRequest.newBuilder()
        .setInstrumentId(instrumentId)
        .build()));
  }

  /**
   * Получение (синхронное) текущего торгового статуса инструментов
   *
   * @param instrumentIds FIGI-идентификатор / uid инструментов.
   * @return текущий торговый статус инструмента
   */
  @Nonnull
  public GetTradingStatusesResponse getTradingStatusesSync(@Nonnull Iterable<String> instrumentIds) {
    return Helpers.unaryCall(() -> marketDataBlockingStub.getTradingStatuses(
      GetTradingStatusesRequest.newBuilder()
        .addAllInstrumentId(instrumentIds)
        .build()));
  }

  /**
   * Получение (синхронное) технических индикаторов по инструменту
   * @param indicatorType Тип технического индикатора
   *                      <p><ul>
   *                      <li>INDICATOR_TYPE_BB - Bollinger Bands (Линия Боллинжера);
   *                      <li>INDICATOR_TYPE_EMA - Exponential Moving Average (EMA, Экспоненциальная скользящая средняя);
   *                      <li>INDICATOR_TYPE_RSI - Relative Strength Index (Индекс относительной силы);
   *                      <li>INDICATOR_TYPE_MACD - Moving Average Convergence/Divergence (Схождение/Расхождение скользящих средних);
   *                      <li>INDICATOR_TYPE_SMA - Simple Moving Average (Простое скользящее среднее);
   *                      </ul><p>
   * @param instrumentUid Уникальный идентификатор инструмента в формате UID
   * @param from Начало запрашиваемого периода в часовом поясе UTC.
   * @param to Окончание запрашиваемого периода в часовом поясе UTC
   * @param interval Интервал за который рассчитывается индикатор.
   * <p>Возможные значения:
   *                      <ul>
   *                      <li>INTERVAL_ONE_MINUTE - 1 минута;
   *                      <li>INTERVAL_FIVE_MINUTES - 5 минут;
   *                      <li>INTERVAL_FIFTEEN_MINUTES - 15 минут;
   *                      <li>INTERVAL_ONE_HOUR - час;
   *                      <li>INTERVAL_ONE_DAY - день;
   *                      <li>INTERVAL_2_MIN - 2 минуты;
   *                      <li>INTERVAL_3_MIN - 3 минуты;
   *                      <li>INTERVAL_10_MIN - 10 минут;
   *                      <li>INTERVAL_30_MIN - 30 минут;
   *                      <li>INTERVAL_2_HOUR - 2 часа;
   *                      <li>INTERVAL_4_HOUR - 4 часа;
   *                      <li>INTERVAL_WEEK - неделя;
   *                      <li>INTERVAL_MONTH - месяц;
   *                 </ul><p>
   * @param typeOfPrice Тип цены, используемый при расчёте индикатора
   * <p>Возможные значения:
   *                      <ul>
   *                      <li>TYPE_OF_PRICE_CLOSE - цена закрытия;
   *                      <li>TYPE_OF_PRICE_OPEN - цена открытия;
   *                      <li>TYPE_OF_PRICE_HIGH - максимальное значение за выбранный интервал;
   *                      <li>TYPE_OF_PRICE_LOW - минимальное значение за выбранный интервал;
   *                      <li>TYPE_OF_PRICE_AVG - (close + open + high + low) / 4;
   *                    </ul><p>
   * @param length Параметр индикатора (период): таймфрейм (торговый период), за который рассчитывается индикатор.
   *               <p><b>Ограничение:</b> &gt;=1 и &lt;= 1000<p>
   * @param deviation Параметр индикатора (отклонение): кол-во стандартных отклонений,
   *                  на которые отступает верхняя и нижняя граница
   *                  <p><b>Ограничение:</b> &gt; 0 и &lt;= 50<p>
   * @param smoothingFastLength Параметр индикатора: короткий период сглаживания для первой
   *                            экспоненциальной скользящей средней (EMA)
   *                            <p><b>Ограничение:</b> &gt;= 1 и &lt;= 1000<p>
   * @param smoothingSlowLength Параметр индикатора: длинный период сглаживания для второй
   *                            экспоненциальной скользящей средней (EMA)
   *                            <p><b>Ограничение:</b> &gt;= 1 и &lt;= 1000<p>
   * @param smoothingSignal Параметр индикатора: период сглаживания для третьей
   *                        экспоненциальной скользящей средней (EMA)
   *                        <p><b>Ограничение:</b> &gt;= 1 и &lt;= 50<p>
   * @return Массив временных меток по UTC (в формате Unix Timestamp), для которых были рассчитаны значения индикатора
   */
  @Nonnull
  public GetTechAnalysisResponse getTechAnalysisSync(@Nonnull GetTechAnalysisRequest.IndicatorType indicatorType,
                                                 @Nonnull String instrumentUid,
                                                 @Nonnull Instant from,
                                                 @Nonnull Instant to,
                                                 @Nonnull GetTechAnalysisRequest.IndicatorInterval interval,
                                                 @Nonnull GetTechAnalysisRequest.TypeOfPrice typeOfPrice,
                                                 @Nullable Integer length,
                                                 @Nullable Quotation deviation,
                                                 @Nullable Integer smoothingFastLength,
                                                 @Nullable Integer smoothingSlowLength,
                                                 @Nullable Integer smoothingSignal) {
    GetTechAnalysisRequest.Builder request = GetTechAnalysisRequest.newBuilder()
      .setIndicatorType(indicatorType)
      .setInstrumentUid(instrumentUid)
      .setFrom(DateUtils.instantToTimestamp(from))
      .setTo(DateUtils.instantToTimestamp(to))
      .setInterval(interval)
      .setTypeOfPrice(typeOfPrice);
    if (length != null) {
      request.setLength(length);
    }
    if (deviation != null) {
      request.setDeviation(GetTechAnalysisRequest.Deviation.newBuilder()
        .setDeviationMultiplier(deviation)
        .build());
    }
    if (smoothingFastLength != null && smoothingSlowLength != null && smoothingSignal != null) {
      request.setSmoothing(GetTechAnalysisRequest.Smoothing.newBuilder()
        .setFastLength(smoothingFastLength)
        .setSlowLength(smoothingSlowLength)
        .setSignalSmoothing(smoothingSignal)
        .build());
    }
    return Helpers.unaryCall(() -> marketDataBlockingStub.getTechAnalysis(request.build()));
  }

  /**
   * Получение (асинхронное) списка свечей по инструменту.
   *
   * @param instrumentId идентификатор инструмента. Может принимать значение FIGI или uid
   * @param from         Начало периода (по UTC).
   * @param to           Окончание периода (по UTC).
   * @param interval     Интервал свечей
   * @return Список свечей
   */
  @Nonnull
  public CompletableFuture<List<HistoricCandle>> getCandles(@Nonnull String instrumentId,
                                                            @Nonnull Instant from,
                                                            @Nonnull Instant to,
                                                            @Nonnull CandleInterval interval) {
    return getCandles(instrumentId, from, to, interval, GetCandlesRequest.CandleSource.CANDLE_SOURCE_UNSPECIFIED);
  }

  /**
   * Получение (асинхронное) списка свечей по инструменту.
   *
   * @param instrumentId идентификатор инструмента. Может принимать значение FIGI или uid
   * @param from         Начало периода (по UTC).
   * @param to           Окончание периода (по UTC).
   * @param interval     Интервал свечей
   * @param source       Источник свечи (только биржевые или все свечи)
   * @return Список свечей
   */
  @Nonnull
  public CompletableFuture<List<HistoricCandle>> getCandles(@Nonnull String instrumentId,
                                                            @Nonnull Instant from,
                                                            @Nonnull Instant to,
                                                            @Nonnull CandleInterval interval,
                                                            @Nonnull GetCandlesRequest.CandleSource source) {
    ValidationUtils.checkFromTo(from, to);

    return Helpers.<GetCandlesResponse>unaryAsyncCall(
        observer -> marketDataStub.getCandles(
          GetCandlesRequest.newBuilder()
            .setInstrumentId(instrumentId)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .setInterval(interval)
            .setCandleSourceType(source)
            .build(),
          observer))
      .thenApply(GetCandlesResponse::getCandlesList);
  }

  /**
   * Получение (асинхронное) списка обезличенных сделок по инструменту.
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @param from         Начало периода (по UTC).
   * @param to           Окончание периода (по UTC).
   * @return Список обезличенных сделок по инструменту.
   */
  @Nonnull
  public CompletableFuture<List<Trade>> getLastTrades(@Nonnull String instrumentId,
                                                      @Nonnull Instant from,
                                                      @Nonnull Instant to) {
    ValidationUtils.checkFromTo(from, to);

    return Helpers.<GetLastTradesResponse>unaryAsyncCall(
        observer -> marketDataStub.getLastTrades(
          GetLastTradesRequest.newBuilder()
            .setInstrumentId(instrumentId)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .build(),
          observer))
      .thenApply(GetLastTradesResponse::getTradesList);
  }

  /**
   * Получение (асинхронное) списка обезличенных сделок по инструменту за последний час.
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента..
   * @return Список обезличенных сделок по инструменту.
   */
  @Nonnull
  public CompletableFuture<List<Trade>> getLastTrades(@Nonnull String instrumentId) {
    var to = Instant.now();
    var from = to.minus(60, ChronoUnit.MINUTES);
    return getLastTrades(instrumentId, from, to);
  }

  /**
   * Получение (асинхронное) списка цен закрытия торговой сессии по инструменту.
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @return Цена закрытия торговой сессии по инструменту.
   */
  @Nonnull
  public CompletableFuture<List<InstrumentClosePriceResponse>> getClosePrices(@Nonnull String instrumentId) {
    var instruments = InstrumentClosePriceRequest.newBuilder().setInstrumentId(instrumentId).build();

    return Helpers.<GetClosePricesResponse>unaryAsyncCall(
        observer -> marketDataStub.getClosePrices(
          GetClosePricesRequest.newBuilder()
            .addAllInstruments(List.of(instruments))
            .build(),
          observer))
      .thenApply(GetClosePricesResponse::getClosePricesList);
  }

  /**
   * Получение (асинхронное) списка цен закрытия торговой сессии по инструментам.
   *
   * @param instrumentIds FIGI-идентификатор / uid инструментов.
   * @return Цена закрытия торговой сессии по инструментам.
   */
  @Nonnull
  public CompletableFuture<List<InstrumentClosePriceResponse>> getClosePrices(@Nonnull Iterable<String> instrumentIds) {
    var instruments = new ArrayList<InstrumentClosePriceRequest>();
    for (String instrumentId : instrumentIds) {
      instruments.add(InstrumentClosePriceRequest.newBuilder().setInstrumentId(instrumentId).build());
    }

    return Helpers.<GetClosePricesResponse>unaryAsyncCall(
        observer -> marketDataStub.getClosePrices(
          GetClosePricesRequest.newBuilder()
            .addAllInstruments(instruments)
            .build(),
          observer))
      .thenApply(GetClosePricesResponse::getClosePricesList);
  }

  /**
   * Получение (асинхронное) списка цен закрытия торговой сессии по инструменту.
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @return Цена закрытия торговой сессии по инструменту.
   */
  @Nonnull
  public List<InstrumentClosePriceResponse> getClosePricesSync(@Nonnull String instrumentId) {
    var instruments = InstrumentClosePriceRequest.newBuilder().setInstrumentId(instrumentId).build();

    return Helpers.unaryCall(() -> marketDataBlockingStub.getClosePrices(
      GetClosePricesRequest.newBuilder()
        .addAllInstruments(List.of(instruments))
        .build()).getClosePricesList());
  }

  /**
   * Получение (асинхронное) списка цен закрытия торговой сессии по инструментам.
   *
   * @param instrumentIds FIGI-идентификатор / uid инструментов.
   * @return Цена закрытия торговой сессии по инструментам.
   */
  @Nonnull
  public List<InstrumentClosePriceResponse> getClosePricesSync(@Nonnull Iterable<String> instrumentIds) {
    var instruments = new ArrayList<InstrumentClosePriceRequest>();
    for (String instrumentId : instrumentIds) {
      instruments.add(InstrumentClosePriceRequest.newBuilder().setInstrumentId(instrumentId).build());
    }

    return Helpers.unaryCall(() -> marketDataBlockingStub.getClosePrices(
      GetClosePricesRequest.newBuilder()
        .addAllInstruments(instruments)
        .build()).getClosePricesList());
  }

  /**
   * Получение (асинхронное) списка последних цен по инструментам
   *
   * @param instrumentIds FIGI-идентификатор / uid инструмента.
   * @return Список последний цен
   */
  @Nonnull
  public CompletableFuture<List<LastPrice>> getLastPrices(@Nonnull Iterable<String> instrumentIds) {
    return Helpers.<GetLastPricesResponse>unaryAsyncCall(
        observer -> marketDataStub.getLastPrices(
          GetLastPricesRequest.newBuilder()
            .addAllInstrumentId(instrumentIds)
            .build(),
          observer))
      .thenApply(GetLastPricesResponse::getLastPricesList);
  }

  /**
   * Получение (асинхронное) списка последних цен по инструментам
   *
   * @param instrumentIds FIGI-идентификатор / uid инструмента.
   * @param lastPriceType Тип запрашиваемой последней цены.
   * @return Список последний цен
   */
  @Nonnull
  public CompletableFuture<List<LastPrice>> getLastPrices(@Nonnull Iterable<String> instrumentIds,
                                                          @Nullable LastPriceType lastPriceType) {
    GetLastPricesRequest.Builder request = GetLastPricesRequest.newBuilder()
      .addAllInstrumentId(instrumentIds);
    if (lastPriceType != null) {
      request.setLastPriceType(lastPriceType);
    }
    return Helpers.<GetLastPricesResponse>unaryAsyncCall(
        observer -> marketDataStub.getLastPrices(
          request.build(),
          observer))
      .thenApply(GetLastPricesResponse::getLastPricesList);
  }

  /**
   * Получение (асинхронное) информации о стакане
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @param depth        глубина стакана
   * @return данные стакана по инструменту
   */
  @Nonnull
  public CompletableFuture<GetOrderBookResponse> getOrderBook(@Nonnull String instrumentId, int depth) {
    return Helpers.unaryAsyncCall(
      observer -> marketDataStub.getOrderBook(
        GetOrderBookRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .setDepth(depth)
          .build(),
        observer));
  }

  /**
   * Получение (асинхронное) информации о торговом статусе инструмента
   *
   * @param instrumentId FIGI-идентификатор / uid инструмента.
   * @return Информация о торговом статусе
   */
  @Nonnull
  public CompletableFuture<GetTradingStatusResponse> getTradingStatus(@Nonnull String instrumentId) {
    return Helpers.unaryAsyncCall(
      observer -> marketDataStub.getTradingStatus(
        GetTradingStatusRequest.newBuilder()
          .setInstrumentId(instrumentId)
          .build(),
        observer));
  }

  /**
   * Получение (асинхронное) информации о торговом статусе инструментов
   *
   * @param instrumentIds FIGI-идентификатор / uid инструментов.
   * @return Информация о торговом статусе
   */
  @Nonnull
  public CompletableFuture<GetTradingStatusesResponse> getTradingStatuses(@Nonnull Iterable<String> instrumentIds) {
    return Helpers.unaryAsyncCall(
      observer -> marketDataStub.getTradingStatuses(
        GetTradingStatusesRequest.newBuilder()
          .addAllInstrumentId(instrumentIds)
          .build(),
        observer));
  }

  /**
   * Получение (синхронное) технических индикаторов по инструменту
   * @param indicatorType Тип технического индикатора
   *                      <p><ul>
   *                      <li>INDICATOR_TYPE_BB - Bollinger Bands (Линия Боллинжера);
   *                      <li>INDICATOR_TYPE_EMA - Exponential Moving Average (EMA, Экспоненциальная скользящая средняя);
   *                      <li>INDICATOR_TYPE_RSI - Relative Strength Index (Индекс относительной силы);
   *                      <li>INDICATOR_TYPE_MACD - Moving Average Convergence/Divergence (Схождение/Расхождение скользящих средних);
   *                      <li>INDICATOR_TYPE_SMA - Simple Moving Average (Простое скользящее среднее);
   *                      </ul><p>
   * @param instrumentUid Уникальный идентификатор инструмента в формате UID
   * @param from Начало запрашиваемого периода в часовом поясе UTC.
   * @param to Окончание запрашиваемого периода в часовом поясе UTC
   * @param interval Интервал за который рассчитывается индикатор.
   * <p>Возможные значения:
   *                      <ul>
   *                      <li>INTERVAL_ONE_MINUTE - 1 минута;
   *                      <li>INTERVAL_FIVE_MINUTES - 5 минут;
   *                      <li>INTERVAL_FIFTEEN_MINUTES - 15 минут;
   *                      <li>INTERVAL_ONE_HOUR - час;
   *                      <li>INTERVAL_ONE_DAY - день;
   *                      <li>INTERVAL_2_MIN - 2 минуты;
   *                      <li>INTERVAL_3_MIN - 3 минуты;
   *                      <li>INTERVAL_10_MIN - 10 минут;
   *                      <li>INTERVAL_30_MIN - 30 минут;
   *                      <li>INTERVAL_2_HOUR - 2 часа;
   *                      <li>INTERVAL_4_HOUR - 4 часа;
   *                      <li>INTERVAL_WEEK - неделя;
   *                      <li>INTERVAL_MONTH - месяц;
   *                 </ul><p>
   * @param typeOfPrice Тип цены, используемый при расчёте индикатора
   * <p>Возможные значения:
   *                      <ul>
   *                      <li>TYPE_OF_PRICE_CLOSE - цена закрытия;
   *                      <li>TYPE_OF_PRICE_OPEN - цена открытия;
   *                      <li>TYPE_OF_PRICE_HIGH - максимальное значение за выбранный интервал;
   *                      <li>TYPE_OF_PRICE_LOW - минимальное значение за выбранный интервал;
   *                      <li>TYPE_OF_PRICE_AVG - (close + open + high + low) / 4;
   *                    </ul><p>
   * @param length Параметр индикатора (период): таймфрейм (торговый период), за который рассчитывается индикатор.
   *               <p><b>Ограничение:</b> &gt;=1 и &lt;= 1000<p>
   * @param deviation Параметр индикатора (отклонение): кол-во стандартных отклонений,
   *                  на которые отступает верхняя и нижняя граница
   *                  <p><b>Ограничение:</b> &gt; 0 и &lt;= 50<p>
   * @param smoothingFastLength Параметр индикатора: короткий период сглаживания для первой
   *                            экспоненциальной скользящей средней (EMA)
   *                            <p><b>Ограничение:</b> &gt;= 1 и &lt;= 1000<p>
   * @param smoothingSlowLength Параметр индикатора: длинный период сглаживания для второй
   *                            экспоненциальной скользящей средней (EMA)
   *                            <p><b>Ограничение:</b> &gt;= 1 и &lt;= 1000<p>
   * @param smoothingSignal Параметр индикатора: период сглаживания для третьей
   *                        экспоненциальной скользящей средней (EMA)
   *                        <p><b>Ограничение:</b> &gt;= 1 и &lt;= 50<p>
   * @return Массив временных меток по UTC (в формате Unix Timestamp), для которых были рассчитаны значения индикатора
   */
  @Nonnull
  public CompletableFuture<GetTechAnalysisResponse> getTechAnalysis(@Nonnull GetTechAnalysisRequest.IndicatorType indicatorType,
                                                 @Nonnull String instrumentUid,
                                                 @Nonnull Instant from,
                                                 @Nonnull Instant to,
                                                 @Nonnull GetTechAnalysisRequest.IndicatorInterval interval,
                                                 @Nonnull GetTechAnalysisRequest.TypeOfPrice typeOfPrice,
                                                 @Nullable Integer length,
                                                 @Nullable Quotation deviation,
                                                 @Nullable Integer smoothingFastLength,
                                                 @Nullable Integer smoothingSlowLength,
                                                 @Nullable Integer smoothingSignal) {
    GetTechAnalysisRequest.Builder request = GetTechAnalysisRequest.newBuilder()
      .setIndicatorType(indicatorType)
      .setInstrumentUid(instrumentUid)
      .setFrom(DateUtils.instantToTimestamp(from))
      .setTo(DateUtils.instantToTimestamp(to))
      .setInterval(interval)
      .setTypeOfPrice(typeOfPrice);
    if (length != null) {
      request.setLength(length);
    }
    if (deviation != null) {
      request.setDeviation(GetTechAnalysisRequest.Deviation.newBuilder()
        .setDeviationMultiplier(deviation)
        .build());
    }
    if (smoothingFastLength != null && smoothingSlowLength != null && smoothingSignal != null) {
      request.setSmoothing(GetTechAnalysisRequest.Smoothing.newBuilder()
        .setFastLength(smoothingFastLength)
        .setSlowLength(smoothingSlowLength)
        .setSignalSmoothing(smoothingSignal)
        .build());
    }
    return Helpers.unaryAsyncCall(observer -> marketDataStub.getTechAnalysis(request.build(), observer));
  }

  /**
   * Получение (асинхронное) информации по техническим индикаторам
   * @param request - запрос тех индикаторов
   * @return результат - значения тех индескаторов
   */
  public CompletableFuture<GetTechAnalysisResponse> getTechAnalysis(@Nonnull GetTechAnalysisRequest request) {
    return Helpers.unaryAsyncCall(
      observer -> marketDataStub.getTechAnalysis(request, observer)
    );
  }

  /**
   * Получение информации по техническим индикаторам
   * @param request - запрос тех индикаторов
   * @return результат - значения тех индескаторов
   */
  public GetTechAnalysisResponse getTechAnalysisSync(@Nonnull GetTechAnalysisRequest request) {
    return Helpers.unaryCall(
      () -> marketDataBlockingStub.getTechAnalysis(request)
    );
  }
}
