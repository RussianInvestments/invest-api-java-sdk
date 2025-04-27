package ru.tinkoff.piapi.core;

import io.grpc.stub.StreamObserver;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.utils.DateUtils;
import ru.tinkoff.piapi.core.utils.Helpers;
import ru.tinkoff.piapi.core.utils.ValidationUtils;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc.InstrumentsServiceBlockingStub;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc.InstrumentsServiceStub;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Сервис предоставления справочной информации о ценных бумагах.
 * <p>
 * Подробности в <a href="https://RussianInvestments.github.io/investAPI/head-instruments/">документации</a>.
 */
@Deprecated(since = "1.30", forRemoval = true)
public class InstrumentsService {

  private final InstrumentsServiceBlockingStub instrumentsBlockingStub;
  private final InstrumentsServiceStub instrumentsStub;

  InstrumentsService(@Nonnull InstrumentsServiceBlockingStub instrumentsBlockingStub,
                     @Nonnull InstrumentsServiceStub instrumentsStub) {
    this.instrumentsBlockingStub = instrumentsBlockingStub;
    this.instrumentsStub = instrumentsStub;
  }

  /**
   * Получение (синхронное) купонов по облигациям.
   *
   * @param figi Тикер облигации.
   * @param from Начало периода по часовому поясу UTC.
   * @param to   Окончание периода по часовому поясу UTC.
   * @return информация о купонах облигации.
   */
  @Nonnull
  public List<Coupon> getBondCouponsSync(@Nonnull String figi,
                                         @Nonnull Instant from,
                                         @Nonnull Instant to) {
    ValidationUtils.checkFromTo(from, to);

    return Helpers.unaryCall(() -> instrumentsBlockingStub.getBondCoupons(
      GetBondCouponsRequest.newBuilder()
        .setFigi(figi)
        .setFrom(DateUtils.instantToTimestamp(from))
        .setTo(DateUtils.instantToTimestamp(to))
        .build()).getEventsList());
  }

  /**
   * Получение (асинхронное) купонов по облигациям.
   *
   * @param figi Тикер облигации.
   * @param from Начало периода по часовому поясу UTC.
   * @param to   Окончание периода по часовому поясу UTC.
   * @return информация о купонах облигации.
   */
  @Nonnull
  public CompletableFuture<List<Coupon>> getBondCoupons(@Nonnull String figi,
                                                        @Nonnull Instant from,
                                                        @Nonnull Instant to) {
    ValidationUtils.checkFromTo(from, to);

    return Helpers.unaryCall(() -> Helpers.<GetBondCouponsResponse>unaryAsyncCall(
        observer -> instrumentsStub.getBondCoupons(
          GetBondCouponsRequest.newBuilder()
            .setFigi(figi)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .build(), observer))
      .thenApply(GetBondCouponsResponse::getEventsList));
  }


  /**
   * Получение (синхронное) расписания торгов <em>всех</em> торговых площадок.
   * <p>
   * Начало периода не должно быть меньше полуночи текущих суток по Московскому времени.
   *
   * @param from Начало периода по часовому поясу UTC.
   * @param to   Окончание периода по часовому поясу UTC.
   * @return Расписания торгов.
   */
  @Nonnull
  public List<TradingSchedule> getTradingSchedulesSync(@Nonnull Instant from,
                                                       @Nonnull Instant to) {
    ValidationUtils.checkFromTo(from, to);

    return Helpers.unaryCall(() -> instrumentsBlockingStub.tradingSchedules(
        TradingSchedulesRequest.newBuilder()
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .build())
      .getExchangesList());
  }

  @Nonnull
  public List<TradingSchedule> getTradingSchedulesSync() {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.tradingSchedules(
        TradingSchedulesRequest.getDefaultInstance())
      .getExchangesList());
  }

  /**
   * Получение (синхронное) расписания торгов торговой площадки.
   * <p>
   * Начало периода не должно быть меньше полуночи текущих суток по Московскому времени.
   *
   * @param exchange Наименование биржи или расчетного календаря.
   * @param from     Начало периода по часовому поясу UTC.
   * @param to       Окончание периода по часовому поясу UTC.
   * @return Расписание торгов площадки (если таковая существует).
   */
  @Nonnull
  public TradingSchedule getTradingScheduleSync(@Nonnull String exchange,
                                                @Nonnull Instant from,
                                                @Nonnull Instant to) {
    ValidationUtils.checkFromTo(from, to);

    return Helpers.unaryCall(() -> instrumentsBlockingStub.tradingSchedules(
        TradingSchedulesRequest.newBuilder()
          .setExchange(exchange)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .build())
      .getExchangesList()
      .get(0));
  }

  /**
   * Получение (синхронное) облигации по тикеру и бирже.
   *
   * @param ticker    Тикер облигации.
   * @param classCode Биржевой класс-код.
   * @return Облигация (если таковая есть).
   */
  @Nonnull
  public Bond getBondByTickerSync(@Nonnull String ticker,
                                  @Nonnull String classCode) {
    return getInstrumentByTickerSync(ticker, classCode, request -> instrumentsBlockingStub.bondBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) облигации по FIGI.
   *
   * @param figi FIGI облигации.
   * @return Облигация (если таковая есть).
   */
  @Nonnull
  public Bond getBondByFigiSync(@Nonnull String figi) {
    return getInstrumentByFigiSync(figi, request -> instrumentsBlockingStub.bondBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) облигации по uid.
   *
   * @param uid Уникальный идентификатор инструмента.
   * @return Облигация (если таковая есть).
   */
  @Nonnull
  public Bond getBondByUidSync(@Nonnull String uid) {
    return getInstrumentByUidSync(uid, request -> instrumentsBlockingStub.bondBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) облигации по positionUid.
   *
   * @param positionUid Уникальный идентификатор позиции.
   * @return Облигация (если таковая есть).
   */
  @Nonnull
  public Bond getBondByPositionUidSync(@Nonnull String positionUid) {
    return getInstrumentByPositionUidSync(positionUid, request -> instrumentsBlockingStub.bondBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) списка облигаций доступных для торговли через Tinkoff Invest API.
   *
   * @return Список облигаций.
   */
  @Nonnull
  public List<Bond> getTradableBondsSync() {
    return getBondsSync(InstrumentStatus.INSTRUMENT_STATUS_BASE);
  }

  /**
   * Получение (синхронное) списка всех облигаций доступных в Тинькофф Инвестиции.
   *
   * @return Список облигаций.
   */
  @Nonnull
  public List<Bond> getAllBondsSync() {
    return getBondsSync(InstrumentStatus.INSTRUMENT_STATUS_ALL);
  }

  /**
   * Получение (синхронное) списка облигаций.
   *
   * @param instrumentStatus статус инструмента. Значения INSTRUMENT_STATUS_BASE, INSTRUMENT_STATUS_ALL
   * @return Список облигаций.
   */
  public List<Bond> getBondsSync(InstrumentStatus instrumentStatus) {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.bonds(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(instrumentStatus)
          .build())
      .getInstrumentsList());
  }

  /**
   * Получение (синхронное) валюты по тикеру и бирже.
   *
   * @param ticker    Тикер валюты.
   * @param classCode Биржевой класс-код.
   * @return Валюта (если таковая есть).
   */
  @Nonnull
  public Currency getCurrencyByTickerSync(@Nonnull String ticker,
                                          @Nonnull String classCode) {
    return getInstrumentByTickerSync(
      ticker,
      classCode,
      request -> instrumentsBlockingStub.currencyBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) валюты по FIGI.
   *
   * @param figi FIGI валюты.
   * @return Валюта (если таковая есть).
   */
  @Nonnull
  public Currency getCurrencyByFigiSync(@Nonnull String figi) {
    return getInstrumentByFigiSync(figi, request -> instrumentsBlockingStub.currencyBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) валюты по uid.
   *
   * @param uid Уникальный идентификатор инструмента.
   * @return Валюта (если таковая есть).
   */
  @Nonnull
  public Currency getCurrencyByUidSync(@Nonnull String uid) {
    return getInstrumentByUidSync(uid, request -> instrumentsBlockingStub.currencyBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) валюты по positionUid.
   *
   * @param positionUid Уникальный идентификатор позиции.
   * @return Валюта (если таковая есть).
   */
  @Nonnull
  public Currency getCurrencyByPositionUidSync(@Nonnull String positionUid) {
    return getInstrumentByPositionUidSync(positionUid, request -> instrumentsBlockingStub.currencyBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) списка валют.
   *
   * @param instrumentStatus статус инструмента. Значения INSTRUMENT_STATUS_BASE, INSTRUMENT_STATUS_ALL
   * @return Список валют.
   */
  public List<Currency> getCurrenciesSync(InstrumentStatus instrumentStatus) {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.currencies(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(instrumentStatus)
          .build())
      .getInstrumentsList());
  }

  /**
   * Получение (синхронное) списка валют доступных для торговли через Tinkoff Invest API.
   *
   * @return Список валют.
   */
  @Nonnull
  public List<Currency> getTradableCurrenciesSync() {
    return getCurrenciesSync(InstrumentStatus.INSTRUMENT_STATUS_BASE);
  }

  /**
   * Получение (синхронное) списка всех валют доступных в Тинькофф Инвестиции.
   *
   * @return Список валют.
   */
  @Nonnull
  public List<Currency> getAllCurrenciesSync() {
    return getCurrenciesSync(InstrumentStatus.INSTRUMENT_STATUS_ALL);
  }

  /**
   * Получение (синхронное) фонда по тикеру и бирже.
   *
   * @param ticker    Тикер фонда.
   * @param classCode Биржевой класс-код.
   * @return Фонд (если таковой есть).
   */
  @Nonnull
  public Etf getEtfByTickerSync(@Nonnull String ticker,
                                @Nonnull String classCode) {
    return getInstrumentByTickerSync(ticker, classCode, request -> instrumentsBlockingStub.etfBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) фонда по FIGI.
   *
   * @param figi FIGI фонда.
   * @return Фонд (если таковой есть).
   */
  @Nonnull
  public Etf getEtfByFigiSync(@Nonnull String figi) {
    return getInstrumentByFigiSync(figi, request -> instrumentsBlockingStub.etfBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) фонда по uid.
   *
   * @param uid Уникальный идентификатор инструмента.
   * @return Фонд (если таковой есть).
   */
  @Nonnull
  public Etf getEtfByUidSync(@Nonnull String uid) {
    return getInstrumentByUidSync(uid, request -> instrumentsBlockingStub.etfBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) фонда по positionUid.
   *
   * @param positionUid Уникальный идентификатор позиции.
   * @return Фонд (если таковой есть).
   */
  @Nonnull
  public Etf getEtfByPositionUidSync(@Nonnull String positionUid) {
    return getInstrumentByPositionUidSync(positionUid, request -> instrumentsBlockingStub.etfBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) списка фондов.
   *
   * @param instrumentStatus статус инструмента. Значения INSTRUMENT_STATUS_BASE, INSTRUMENT_STATUS_ALL
   * @return Список фондов.
   */
  public List<Etf> getEtfsSync(InstrumentStatus instrumentStatus) {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.etfs(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(instrumentStatus)
          .build())
      .getInstrumentsList());
  }

  /**
   * Получение (синхронное) списка фондов.
   *
   * @param instrumentStatus статус инструмента. Значения INSTRUMENT_STATUS_BASE, INSTRUMENT_STATUS_ALL
   * @param exchangeType Площадка, на которой торгуется инструмент, возможные значения.
   *                     Значения INSTRUMENT_EXCHANGE_UNSPECIFIED - не определено,
   *                     INSTRUNENT_EXCHANGE_DEALER - торговля внутри дилера;
   * @return Список фондов.
   */
  public List<Etf> getEtfsSync(InstrumentStatus instrumentStatus, InstrumentExchangeType exchangeType) {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.etfs(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(instrumentStatus)
          .setInstrumentExchange(exchangeType)
          .build())
      .getInstrumentsList());
  }

  /**
   * Получение (синхронное) списка фондов доступных для торговли через Tinkoff Invest API.
   *
   * @return Список фондов.
   */
  @Nonnull
  public List<Etf> getTradableEtfsSync() {
    return getEtfsSync(InstrumentStatus.INSTRUMENT_STATUS_BASE);
  }

  /**
   * Получение (синхронное) списка всех фондов доступных в Тинькофф Инвестиции.
   *
   * @return Список фондов.
   */
  @Nonnull
  public List<Etf> getAllEtfsSync() {
    return getEtfsSync(InstrumentStatus.INSTRUMENT_STATUS_ALL);
  }

  /**
   * Получение (синхронное) фьючерса по тикеру и бирже.
   *
   * @param ticker    Тикер фьючерса.
   * @param classCode Биржевой класс-код.
   * @return Фьючерс (если таковой есть).
   */
  @Nonnull
  public Future getFutureByTickerSync(@Nonnull String ticker,
                                      @Nonnull String classCode) {
    return getInstrumentByTickerSync(
      ticker,
      classCode,
      request -> instrumentsBlockingStub.futureBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) фьючерса по FIGI.
   *
   * @param figi FIGI фьючерса.
   * @return Фьючерс (если таковой есть).
   */
  @Nonnull
  public Future getFutureByFigiSync(@Nonnull String figi) {
    return getInstrumentByFigiSync(figi, request -> instrumentsBlockingStub.futureBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) фьючерса по uid.
   *
   * @param uid Уникальный идентификатор инструмента.
   * @return Фьючерс (если таковой есть).
   */
  @Nonnull
  public Future getFutureByUidSync(@Nonnull String uid) {
    return getInstrumentByUidSync(uid, request -> instrumentsBlockingStub.futureBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) фьючерса по positionUid.
   *
   * @param positionUid Уникальный идентификатор позиции.
   * @return Фьючерс (если таковой есть).
   */
  @Nonnull
  public Future getFutureByPositionUidSync(@Nonnull String positionUid) {
    return getInstrumentByPositionUidSync(positionUid, request -> instrumentsBlockingStub.futureBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) списка фьючерсов.
   *
   * @param instrumentStatus статус инструмента. Значения INSTRUMENT_STATUS_BASE, INSTRUMENT_STATUS_ALL
   * @return Список фьючерсов.
   */
  public List<Future> getFuturesSync(InstrumentStatus instrumentStatus) {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.futures(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(instrumentStatus)
          .build())
      .getInstrumentsList());
  }

  /**
   * Получение (синхронное) списка фьючерсов доступных для торговли через Tinkoff Invest API.
   *
   * @return Список фьючерсов.
   */
  @Nonnull
  public List<Future> getTradableFuturesSync() {
    return getFuturesSync(InstrumentStatus.INSTRUMENT_STATUS_BASE);
  }

  /**
   * Получение (синхронное) списка всех фьючерсов доступных в Тинькофф Инвестиции.
   *
   * @return Список фондов.
   */
  @Nonnull
  public List<Future> getAllFuturesSync() {
    return getFuturesSync(InstrumentStatus.INSTRUMENT_STATUS_ALL);
  }

  /**
   * Получение (синхронное) акции по тикеру и бирже.
   *
   * @param ticker    Тикер акции.
   * @param classCode Биржевой класс-код.
   * @return Акция (если таковой есть).
   */
  @Nonnull
  public Share getShareByTickerSync(@Nonnull String ticker,
                                    @Nonnull String classCode) {
    return getInstrumentByTickerSync(
      ticker,
      classCode,
      request -> instrumentsBlockingStub.shareBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) акции по FIGI.
   *
   * @param figi FIGI акции.
   * @return Акция (если таковой есть).
   */
  @Nonnull
  public Share getShareByFigiSync(@Nonnull String figi) {
    return getInstrumentByFigiSync(figi, request -> instrumentsBlockingStub.shareBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) акции по uid.
   *
   * @param uid Уникальный идентификатор инструмента.
   * @return Акция (если таковой есть).
   */
  @Nonnull
  public Share getShareByUidSync(@Nonnull String uid) {
    return getInstrumentByUidSync(uid, request -> instrumentsBlockingStub.shareBy(request).getInstrument());
  }


  /**
   * Получение (синхронное) акции по positionUid.
   *
   * @param positionUid Уникальный идентификатор позиции.
   * @return Акция (если таковой есть).
   */
  @Nonnull
  public Share getShareByPositionUidSync(@Nonnull String positionUid) {
    return getInstrumentByPositionUidSync(positionUid, request -> instrumentsBlockingStub.shareBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) списка акций.
   *
   * @param instrumentStatus статус инструмента. Значения INSTRUMENT_STATUS_BASE, INSTRUMENT_STATUS_ALL
   * @return Список акций.
   */
  public List<Share> getSharesSync(InstrumentStatus instrumentStatus) {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.shares(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(instrumentStatus)
          .build())
      .getInstrumentsList());
  }

  /**
   * Получение (синхронное) списка акций.
   *
   * @param instrumentStatus статус инструмента. Значения INSTRUMENT_STATUS_BASE, INSTRUMENT_STATUS_ALL
   * @param exchangeType Площадка, на которой торгуется инструмент, возможные значения.
   *                     Значения INSTRUMENT_EXCHANGE_UNSPECIFIED - не определено,
   * INSTRUNENT_EXCHANGE_DEALER - торговля внутри дилера;
   * @return Список акций.
   */
  public List<Share> getSharesSync(InstrumentStatus instrumentStatus, InstrumentExchangeType exchangeType) {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.shares(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(instrumentStatus)
          .setInstrumentExchange(exchangeType)
          .build())
      .getInstrumentsList());
  }

  /**
   * Получение (синхронное) списка акций доступных для торговли через Tinkoff Invest API.
   *
   * @return Список акций.
   */
  @Nonnull
  public List<Share> getTradableSharesSync() {
    return getSharesSync(InstrumentStatus.INSTRUMENT_STATUS_BASE);
  }

  /**
   * Получение (синхронное) списка всех акций доступных в Тинькофф Инвестиции.
   *
   * @return Список акций.
   */
  @Nonnull
  public List<Share> getAllSharesSync() {
    return getSharesSync(InstrumentStatus.INSTRUMENT_STATUS_ALL);
  }

  /**
   * Получение (синхронное) накопленного купонного дохода по облигации.
   *
   * @param figi FIGI облигации.
   * @param from Начало периода по часовому поясу UTC.
   * @param to   Конец периода по часовому поясу UTC.
   * @return НКД по облигации (если есть).
   */
  @Nonnull
  public List<AccruedInterest> getAccruedInterestsSync(@Nonnull String figi,
                                                       @Nonnull Instant from,
                                                       @Nonnull Instant to) {
    ValidationUtils.checkFromTo(from, to);

    return Helpers.unaryCall(() -> instrumentsBlockingStub.getAccruedInterests(
        GetAccruedInterestsRequest.newBuilder()
          .setFigi(figi)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .build())
      .getAccruedInterestsList());

  }

  /**
   * Получение (синхронное) размера гарантийного обеспечения по фьючерсам.
   *
   * @param figi figi / instrument_uid инструмента.
   * @return Размер гарантийного обеспечения по фьючерсу (если есть).
   */
  @Nonnull
  public GetFuturesMarginResponse getFuturesMarginSync(@Nonnull String figi) {

    return Helpers.unaryCall(() -> instrumentsBlockingStub.getFuturesMargin(
      GetFuturesMarginRequest.newBuilder()
        .setInstrumentId(figi)
        .build()));
  }

  /**
   * Получение (синхронное) основной информации об инструменте.
   *
   * @param ticker    Тикер инструмента.
   * @param classCode Биржевой класс-код.
   * @return Основная информация об инструменте (если есть).
   */
  @Nonnull
  public Instrument getInstrumentByTickerSync(@Nonnull String ticker,
                                              @Nonnull String classCode) {
    return getInstrumentByTickerSync(ticker, classCode, request -> instrumentsBlockingStub.getInstrumentBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) основной информации об инструменте.
   *
   * @param figi FIGI инструмента.
   * @return Основная информация об инструменте (если есть).
   */
  @Nonnull
  public Instrument getInstrumentByFigiSync(@Nonnull String figi) {
    return getInstrumentByFigiSync(figi, request -> instrumentsBlockingStub.getInstrumentBy(request).getInstrument());
  }

  /**
   * Получение (синхронное) событий выплаты дивидендов по инструменту.
   *
   * @param figi FIGI инструмента.
   * @param from Начало периода по часовому поясу UTC.
   * @param to   Конец периода по часовому поясу UTC.
   * @return События выплаты дивидендов по инструменту (если есть).
   */
  @Nonnull
  public List<Dividend> getDividendsSync(@Nonnull String figi,
                                         @Nonnull Instant from,
                                         @Nonnull Instant to) {
    ValidationUtils.checkFromTo(from, to);

    return Helpers.unaryCall(() -> instrumentsBlockingStub.getDividends(
        GetDividendsRequest.newBuilder()
          .setFigi(figi)
          .setFrom(DateUtils.instantToTimestamp(from))
          .setTo(DateUtils.instantToTimestamp(to))
          .build())
      .getDividendsList());
  }

  /**
   * Получение (синхронное) списка активов.
   *
   * @return Список активов.
   */
  @Nonnull
  public List<Asset> getAssetsSync() {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.getAssets(AssetsRequest.getDefaultInstance()).getAssetsList());
  }


  /**
   * Получение (асинхронное) списка активов.
   *
   * @return Список активов.
   */
  @Nonnull
  public CompletableFuture<List<Asset>> getAssets() {
    return Helpers.<AssetsResponse>unaryAsyncCall(
        observer -> instrumentsStub.getAssets(AssetsRequest.getDefaultInstance(), observer))
      .thenApply(AssetsResponse::getAssetsList);
  }

  /**
   * Получение (синхронное) актива по его идентификатору.
   *
   * @return Данные по активу.
   */
  @Nonnull
  public AssetFull getAssetBySync(String uid) {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.getAssetBy(AssetRequest.newBuilder().setId(uid).build()).getAsset());
  }

  /**
   * Получение (асинхронное) актива по его идентификатору.
   *
   * @return Данные по активу.
   */
  @Nonnull
  public CompletableFuture<AssetFull> getAssetBy(String uid) {
    return Helpers.<AssetResponse>unaryAsyncCall(observer -> instrumentsStub.getAssetBy(AssetRequest.newBuilder().setId(uid).build(), observer))
      .thenApply(AssetResponse::getAsset);
  }

  /**
   * Получение (асинхронное) расписания торгов <em>всех</em> торговых площадок.
   * <p>
   * Начало периода не должно быть меньше полуночи текущих суток по Московскому времени.
   *
   * @param from Начало периода по часовому поясу UTC.
   * @param to   Окончание периода по часовому поясу UTC.
   * @return Расписания торгов.
   */
  @Nonnull
  public CompletableFuture<List<TradingSchedule>> getTradingSchedules(@Nonnull Instant from,
                                                                      @Nonnull Instant to) {
    ValidationUtils.checkFromTo(from, to);

    return Helpers.<TradingSchedulesResponse>unaryAsyncCall(
        observer -> instrumentsStub.tradingSchedules(
          TradingSchedulesRequest.newBuilder()
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .build(),
          observer))
      .thenApply(TradingSchedulesResponse::getExchangesList);
  }

  @Nonnull
  public CompletableFuture<List<TradingSchedule>> getTradingSchedules() {
    return Helpers.<TradingSchedulesResponse>unaryAsyncCall(
        observer -> instrumentsStub.tradingSchedules(
          TradingSchedulesRequest.getDefaultInstance(),
          observer))
      .thenApply(TradingSchedulesResponse::getExchangesList);
  }

  /**
   * Получение (асинхронное) расписания торгов торговой площадки.
   * <p>
   * Начало периода не должно быть меньше полуночи текущих суток по Московскому времени.
   *
   * @param exchange Наименование биржи или расчетного календаря.
   * @param from     Начало периода по часовому поясу UTC.
   * @param to       Окончание периода по часовому поясу UTC.
   * @return Расписание торгов площадки (если таковая существует).
   */
  @Nonnull
  public CompletableFuture<TradingSchedule> getTradingSchedule(@Nonnull String exchange,
                                                               @Nonnull Instant from,
                                                               @Nonnull Instant to) {
    ValidationUtils.checkFromTo(from, to);

    return Helpers.unaryCall(() -> Helpers.<TradingSchedulesResponse>unaryAsyncCall(
        observer -> instrumentsStub.tradingSchedules(
          TradingSchedulesRequest.newBuilder()
            .setExchange(exchange)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .build(),
          observer))
      .thenApply(x -> x.getExchangesList().get(0)));
  }

  /**
   * Получение (асинхронное) облигации по тикеру и бирже.
   *
   * @param ticker    Тикер облигации.
   * @param classCode Биржевой класс-код.
   * @return Облигация (если таковая есть).
   */
  @Nonnull
  public CompletableFuture<Bond> getBondByTicker(@Nonnull String ticker,
                                                 @Nonnull String classCode) {
    return getInstrumentByTicker(ticker, classCode, instrumentsStub::bondBy, BondResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) облигации по FIGI.
   *
   * @param figi FIGI облигации.
   * @return Облигация (если таковая есть).
   */
  @Nonnull
  public CompletableFuture<Bond> getBondByFigi(@Nonnull String figi) {
    return getInstrumentByFigi(figi, instrumentsStub::bondBy, BondResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) облигации по uid.
   *
   * @param uid Уникальный идентификатор инструмента.
   * @return Облигация (если таковая есть).
   */
  @Nonnull
  public CompletableFuture<Bond> getBondByUid(@Nonnull String uid) {
    return getInstrumentByUid(uid, instrumentsStub::bondBy, BondResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) облигации по positionUid.
   *
   * @param positionUid Уникальный идентификатор позиции.
   * @return Облигация (если таковая есть).
   */
  @Nonnull
  public CompletableFuture<Bond> getBondByPositionUid(@Nonnull String positionUid) {
    return getInstrumentByPositionUid(positionUid, instrumentsStub::bondBy, BondResponse::getInstrument);
  }


  /**
   * Получение (асинхронное) списка облигаций.
   *
   * @param instrumentStatus статус инструмента. Значения INSTRUMENT_STATUS_BASE, INSTRUMENT_STATUS_ALL
   * @return Список облигаций.
   */
  public CompletableFuture<List<Bond>> getBonds(InstrumentStatus instrumentStatus) {
    return Helpers.<BondsResponse>unaryAsyncCall(
        observer -> instrumentsStub.bonds(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(instrumentStatus)
            .build(),
          observer))
      .thenApply(BondsResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) списка облигаций доступных для торговли через Tinkoff Invest API.
   *
   * @return Список облигаций.
   */
  @Nonnull
  public CompletableFuture<List<Bond>> getTradableBonds() {
    return getBonds(InstrumentStatus.INSTRUMENT_STATUS_BASE);
  }

  /**
   * Получение (асинхронное) списка всех облигаций доступных в Тинькофф Инвестиции.
   *
   * @return Список облигаций.
   */
  @Nonnull
  public CompletableFuture<List<Bond>> getAllBonds() {
    return getBonds(InstrumentStatus.INSTRUMENT_STATUS_ALL);
  }

  /**
   * Получение (асинхронное) валюты по тикеру и бирже.
   *
   * @param ticker    Тикер валюты.
   * @param classCode Биржевой класс-код.
   * @return Валюта (если таковая есть).
   */
  @Nonnull
  public CompletableFuture<Currency> getCurrencyByTicker(@Nonnull String ticker,
                                                         @Nonnull String classCode) {
    return getInstrumentByTicker(
      ticker,
      classCode,
      instrumentsStub::currencyBy,
      CurrencyResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) валюты по FIGI.
   *
   * @param figi FIGI валюты.
   * @return Валюта (если таковая есть).
   */
  @Nonnull
  public CompletableFuture<Currency> getCurrencyByFigi(@Nonnull String figi) {
    return getInstrumentByFigi(figi, instrumentsStub::currencyBy, CurrencyResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) валюты по uid.
   *
   * @param uid Уникальный идентификатор инструмента.
   * @return Валюта (если таковая есть).
   */
  @Nonnull
  public CompletableFuture<Currency> getCurrencyByUid(@Nonnull String uid) {
    return getInstrumentByUid(uid, instrumentsStub::currencyBy, CurrencyResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) валюты по positionUid.
   *
   * @param positionUid Уникальный идентификатор позиции.
   * @return Валюта (если таковая есть).
   */
  @Nonnull
  public CompletableFuture<Currency> getCurrencyByPositionUid(@Nonnull String positionUid) {
    return getInstrumentByPositionUid(positionUid, instrumentsStub::currencyBy, CurrencyResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) списка валют.
   *
   * @param instrumentStatus статус инструмента. Значения INSTRUMENT_STATUS_BASE, INSTRUMENT_STATUS_ALL
   * @return Список валют.
   */
  public CompletableFuture<List<Currency>> getCurrencies(InstrumentStatus instrumentStatus) {
    return Helpers.<CurrenciesResponse>unaryAsyncCall(
        observer -> instrumentsStub.currencies(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(instrumentStatus)
            .build(),
          observer))
      .thenApply(CurrenciesResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) списка валют доступных для торговли через Tinkoff Invest API.
   *
   * @return Список валют.
   */
  @Nonnull
  public CompletableFuture<List<Currency>> getTradableCurrencies() {
    return getCurrencies(InstrumentStatus.INSTRUMENT_STATUS_BASE);
  }

  /**
   * Получение (асинхронное) списка всех вслют доступных в Тинькофф Инвестиции.
   *
   * @return Список валют.
   */
  @Nonnull
  public CompletableFuture<List<Currency>> getAllCurrencies() {
    return getCurrencies(InstrumentStatus.INSTRUMENT_STATUS_ALL);
  }

  /**
   * Получение (асинхронное) фонда по тикеру и бирже.
   *
   * @param ticker    Тикер фонда.
   * @param classCode Биржевой класс-код.
   * @return Фонд (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Etf> getEtfByTicker(@Nonnull String ticker,
                                               @Nonnull String classCode) {
    return getInstrumentByTicker(ticker, classCode, instrumentsStub::etfBy, EtfResponse::getInstrument);
  }

  /**
   * Получение (синхронное) фонда по FIGI.
   *
   * @param figi FIGI фонда.
   * @return Фонд (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Etf> getEtfByFigi(@Nonnull String figi) {
    return getInstrumentByFigi(figi, instrumentsStub::etfBy, EtfResponse::getInstrument);
  }

  /**
   * Получение (синхронное) фонда по uid.
   *
   * @param uid Уникальный идентификатор инструмента.
   * @return Фонд (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Etf> getEtfByUid(@Nonnull String uid) {
    return getInstrumentByUid(uid, instrumentsStub::etfBy, EtfResponse::getInstrument);
  }

  /**
   * Получение (синхронное) фонда по positionUid.
   *
   * @param positionUid Уникальный идентификатор позиции.
   * @return Фонд (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Etf> getEtfByPositionUid(@Nonnull String positionUid) {
    return getInstrumentByPositionUid(positionUid, instrumentsStub::etfBy, EtfResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) списка фондов.
   *
   * @param instrumentStatus статус инструмента. Значения INSTRUMENT_STATUS_BASE, INSTRUMENT_STATUS_ALL
   * @return Список фондов.
   */
  public CompletableFuture<List<Etf>> getEtfs(InstrumentStatus instrumentStatus) {
    return Helpers.<EtfsResponse>unaryAsyncCall(
        observer -> instrumentsStub.etfs(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(instrumentStatus)
            .build(),
          observer))
      .thenApply(EtfsResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) списка фондов.
   *
   * @param instrumentStatus статус инструмента. Значения INSTRUMENT_STATUS_BASE, INSTRUMENT_STATUS_ALL
   * @param exchangeType Площадка, на которой торгуется инструмент, возможные значения.
   *                     Значения INSTRUMENT_EXCHANGE_UNSPECIFIED - не определено,
   *                     INSTRUNENT_EXCHANGE_DEALER - торговля внутри дилера;
   * @return Список фондов.
   */
  public CompletableFuture<List<Etf>> getEtfs(InstrumentStatus instrumentStatus, InstrumentExchangeType exchangeType) {
    return Helpers.<EtfsResponse>unaryAsyncCall(
        observer -> instrumentsStub.etfs(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(instrumentStatus)
            .setInstrumentExchange(exchangeType)
            .build(),
          observer))
      .thenApply(EtfsResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) списка фондов доступных для торговли через Tinkoff Invest API.
   *
   * @return Список фондов.
   */
  @Nonnull
  public CompletableFuture<List<Etf>> getTradableEtfs() {
    return getEtfs(InstrumentStatus.INSTRUMENT_STATUS_BASE);
  }

  /**
   * Получение (асинхронное) списка всех фондов доступных в Тинькофф Инвестиции.
   *
   * @return Список фондов.
   */
  @Nonnull
  public CompletableFuture<List<Etf>> getAllEtfs() {
    return getEtfs(InstrumentStatus.INSTRUMENT_STATUS_ALL);
  }

  /**
   * Получение (асинхронное) фьючерса по тикеру и бирже.
   *
   * @param ticker    Тикер фьючерса.
   * @param classCode Биржевой класс-код.
   * @return Фьючерс (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Future> getFutureByTicker(@Nonnull String ticker,
                                                     @Nonnull String classCode) {
    return getInstrumentByTicker(ticker, classCode, instrumentsStub::futureBy, FutureResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) фьючерса по FIGI.
   *
   * @param figi FIGI фьючерса.
   * @return Фьючерс (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Future> getFutureByFigi(@Nonnull String figi) {
    return getInstrumentByFigi(figi, instrumentsStub::futureBy, FutureResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) фьючерса по uid.
   *
   * @param uid Уникальный идентификатор инструмента.
   * @return Фьючерс (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Future> getFutureByUid(@Nonnull String uid) {
    return getInstrumentByUid(uid, instrumentsStub::futureBy, FutureResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) фьючерса по positionUid.
   *
   * @param positionUid Уникальный идентификатор позиции.
   * @return Фьючерс (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Future> getFutureByPositionUid(@Nonnull String positionUid) {
    return getInstrumentByPositionUid(positionUid, instrumentsStub::futureBy, FutureResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) списка фьючерсов.
   *
   * @param instrumentStatus статус инструмента. Значения INSTRUMENT_STATUS_BASE, INSTRUMENT_STATUS_ALL
   * @return Список фьючерсов.
   */
  public CompletableFuture<List<Future>> getFutures(InstrumentStatus instrumentStatus) {
    return Helpers.<FuturesResponse>unaryAsyncCall(
        observer -> instrumentsStub.futures(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(instrumentStatus)
            .build(),
          observer))
      .thenApply(FuturesResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) списка фьючерсов доступных для торговли через Tinkoff Invest API.
   *
   * @return Список фьючерсов.
   */
  @Nonnull
  public CompletableFuture<List<Future>> getTradableFutures() {
    return getFutures(InstrumentStatus.INSTRUMENT_STATUS_BASE);
  }

  /**
   * Получение (асинхронное) списка всех фьючерсов доступных в Тинькофф Инвестиции.
   *
   * @return Список фондов.
   */
  @Nonnull
  public CompletableFuture<List<Future>> getAllFutures() {
    return getFutures(InstrumentStatus.INSTRUMENT_STATUS_ALL);
  }

  /**
   * Получение (асинхронное) акции по тикеру и бирже.
   *
   * @param ticker    Тикер акции.
   * @param classCode Биржевой класс-код.
   * @return Акция (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Share> getShareByTicker(@Nonnull String ticker,
                                                   @Nonnull String classCode) {
    return getInstrumentByTicker(ticker, classCode, instrumentsStub::shareBy, ShareResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) акции по FIGI.
   *
   * @param figi FIGI акции.
   * @return Акция (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Share> getShareByFigi(@Nonnull String figi) {
    return getInstrumentByFigi(figi, instrumentsStub::shareBy, ShareResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) акции по uid.
   *
   * @param uid Уникальный идентификатор инструмента.
   * @return Акция (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Share> getShareByUid(@Nonnull String uid) {
    return getInstrumentByUid(uid, instrumentsStub::shareBy, ShareResponse::getInstrument);
  }


  /**
   * Получение (асинхронное) акции по positionUid.
   *
   * @param positionUid Уникальный идентификатор позиции.
   * @return Акция (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Share> getShareByPositionUid(@Nonnull String positionUid) {
    return getInstrumentByPositionUid(positionUid, instrumentsStub::shareBy, ShareResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) списка акций.
   *
   * @param instrumentStatus статус инструмента. Значения INSTRUMENT_STATUS_BASE, INSTRUMENT_STATUS_ALL
   * @return Список акций.
   */
  public CompletableFuture<List<Share>> getShares(InstrumentStatus instrumentStatus) {
    return Helpers.<SharesResponse>unaryAsyncCall(
        observer -> instrumentsStub.shares(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(instrumentStatus)
            .build(),
          observer))
      .thenApply(SharesResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) списка акций.
   *
   * @param instrumentStatus статус инструмента. Значения INSTRUMENT_STATUS_BASE, INSTRUMENT_STATUS_ALL
   * @param exchangeType Площадка, на которой торгуется инструмент, возможные значения.
   *                     Значения INSTRUMENT_EXCHANGE_UNSPECIFIED - не определено,
   *                     INSTRUNENT_EXCHANGE_DEALER - торговля внутри дилера;
   * @return Список акций.
   */
  public CompletableFuture<List<Share>> getShares(InstrumentStatus instrumentStatus,
                                                  InstrumentExchangeType exchangeType) {
    return Helpers.<SharesResponse>unaryAsyncCall(
        observer -> instrumentsStub.shares(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(instrumentStatus)
            .setInstrumentExchange(exchangeType)
            .build(),
          observer))
      .thenApply(SharesResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) списка акций доступных для торговли через Tinkoff Invest API.
   *
   * @return Список акций.
   */
  @Nonnull
  public CompletableFuture<List<Share>> getTradableShares() {
    return getShares(InstrumentStatus.INSTRUMENT_STATUS_BASE);
  }

  /**
   * Получение (асинхронное) списка всех акций доступных в Тинькофф Инвестиции.
   *
   * @return Список акций.
   */
  @Nonnull
  public CompletableFuture<List<Share>> getAllShares() {
    return getShares(InstrumentStatus.INSTRUMENT_STATUS_ALL);
  }

  /**
   * Получение (асинхронное) списка опционов.
   *
   * @param instrumentStatus статус инструмента. Значения INSTRUMENT_STATUS_BASE, INSTRUMENT_STATUS_ALL
   * @return Список опционов.
   */
  public CompletableFuture<List<Option>> getOptions(InstrumentStatus instrumentStatus) {
    return Helpers.<OptionsResponse>unaryAsyncCall(
        observer -> instrumentsStub.options(
          InstrumentsRequest.newBuilder()
            .setInstrumentStatus(instrumentStatus)
            .build(),
          observer))
      .thenApply(OptionsResponse::getInstrumentsList);
  }

  /**
   * Получение (асинхронное) списка опционов доступных для торговли через Tinkoff Invest API.
   *
   * @return Список опционов.
   */
  @Nonnull
  public CompletableFuture<List<Option>> getTradableOptions() {
    return getOptions(InstrumentStatus.INSTRUMENT_STATUS_BASE);
  }

  /**
   * Получение (асинхронное) списка всех опционов доступных в Тинькофф Инвестиции.
   *
   * @return Список опционов.
   */
  @Nonnull
  public CompletableFuture<List<Option>> getAllOptions() {
    return getOptions(InstrumentStatus.INSTRUMENT_STATUS_ALL);
  }

  /**
   * Получение (синхронное) списка опционов.
   *
   * @param instrumentStatus статус инструмента. Значения INSTRUMENT_STATUS_BASE, INSTRUMENT_STATUS_ALL
   * @return Список опционов.
   */
  public List<Option> getOptionsSync(InstrumentStatus instrumentStatus) {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.options(
        InstrumentsRequest.newBuilder()
          .setInstrumentStatus(instrumentStatus)
          .build())
      .getInstrumentsList());
  }

  /**
   * Получение (синхронное) списка опционов доступных для торговли через Tinkoff Invest API.
   *
   * @return Список опционов.
   */
  @Nonnull
  public List<Option> getTradableOptionsSync() {
    return getOptionsSync(InstrumentStatus.INSTRUMENT_STATUS_BASE);
  }

  /**
   * Получение (синхронное) списка всех опционов доступных в Тинькофф Инвестиции.
   *
   * @return Список опционов.
   */
  @Nonnull
  public List<Option> getAllOptionsSync() {
    return getOptionsSync(InstrumentStatus.INSTRUMENT_STATUS_ALL);
  }

  /**
   * Получение (синхронное) опциона по uid.
   *
   * @param uid Уникальный идентификатор инструмента.
   * @return Опцион (если таковой есть).
   */
  @Nonnull
  public Option getOptionByUidSync(@Nonnull String uid) {
    return getInstrumentByUidSync(uid, request->instrumentsBlockingStub.optionBy(request).getInstrument());
  }

  /**
   * Получение (асинхронное) опциона по uid.
   *
   * @param uid Уникальный идентификатор инструмента.
   * @return Опцион (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Option> getOptionByUid(@Nonnull String uid) {
    return getInstrumentByUid(uid, instrumentsStub::optionBy, OptionResponse::getInstrument);
  }

  /**
   * Получение (синхронное) опциона по positionUid.
   *
   * @param positionUid Уникальный идентификатор позиции.
   * @return Опцион (если таковой есть).
   */
  @Nonnull
  public Option getOptionByPositionUidSync(@Nonnull String positionUid) {
    return getInstrumentByPositionUidSync(positionUid, request->instrumentsBlockingStub.optionBy(request).getInstrument());
  }

  /**
   * Получение (асинхронное) опциона по positionUid.
   *
   * @param positionUid Уникальный идентификатор позиции.
   * @return Опцион (если таковой есть).
   */
  @Nonnull
  public CompletableFuture<Option> getOptionByPositionUid(@Nonnull String positionUid) {
    return getInstrumentByPositionUid(positionUid, instrumentsStub::optionBy, OptionResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) накопленного купонного дохода по облигации.
   *
   * @param figi FIGI облигации.
   * @param from Начало периода по часовому поясу UTC.
   * @param to   Конец периода по часовому поясу UTC.
   * @return НКД по облигации (если есть).
   */
  @Nonnull
  public CompletableFuture<List<AccruedInterest>> getAccruedInterests(@Nonnull String figi,
                                                                      @Nonnull Instant from,
                                                                      @Nonnull Instant to) {
    ValidationUtils.checkFromTo(from, to);

    return Helpers.<GetAccruedInterestsResponse>unaryAsyncCall(
        observer -> instrumentsStub.getAccruedInterests(
          GetAccruedInterestsRequest.newBuilder()
            .setFigi(figi)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .build(),
          observer))
      .thenApply(GetAccruedInterestsResponse::getAccruedInterestsList);
  }

  /**
   * Получение (асинхронное) размера гарантийного обеспечения по фьючерсам.
   *
   * @param figi figi / instrument_uid инструмента.
   * @return Размер гарантийного обеспечения по фьючерсу (если есть).
   */
  @Nonnull
  public CompletableFuture<GetFuturesMarginResponse> getFuturesMargin(@Nonnull String figi) {
    return Helpers.unaryAsyncCall(
      observer -> instrumentsStub.getFuturesMargin(
        GetFuturesMarginRequest.newBuilder()
          .setInstrumentId(figi)
          .build(),
        observer));
  }

  /**
   * Получение (асинхронное) основной информации об инструменте.
   *
   * @param ticker    Тикер инструмента.
   * @param classCode Биржевой класс-код.
   * @return Основная информация об инструменте (если есть).
   */
  @Nonnull
  public CompletableFuture<Instrument> getInstrumentByTicker(@Nonnull String ticker,
                                                             @Nonnull String classCode) {
    return getInstrumentByTicker(ticker, classCode, instrumentsStub::getInstrumentBy, InstrumentResponse::getInstrument);
  }

  /**
   * Получение (асинхронное) основной информации об инструменте.
   *
   * @param figi FIGI инструмента.
   * @return Основная информация об инструменте (если есть).
   */
  @Nonnull
  public CompletableFuture<Instrument> getInstrumentByFigi(@Nonnull String figi) {
    return getInstrumentByFigi(figi, instrumentsStub::getInstrumentBy, InstrumentResponse::getInstrument);
  }


  /**
   * Получение (асинхронное) основной информации об инструменте.
   *
   * @param instrumentUid UID инструмента.
   * @return Основная информация об инструменте (если есть).
   */
  @Nonnull
  public CompletableFuture<InstrumentResponse> getInstrumentByUID(@Nonnull String instrumentUid) {
    return getInstrumentByUid(instrumentUid, instrumentsStub::getInstrumentBy, Function.identity());
  }


  /**
   * Получение (sync) основной информации об инструменте.
   *
   * @param instrumentUid UID инструмента.
   * @return Основная информация об инструменте (если есть).
   */
  @Nonnull
  public InstrumentResponse getInstrumentByUIDSync(@Nonnull String instrumentUid) {
    return getInstrumentByUidSync(instrumentUid, instrumentsBlockingStub::getInstrumentBy);
  }


  /**
   * Получение (асинхронное) событий выплаты дивидендов по инструменту.
   *
   * @param figi FIGI инструмента.
   * @param from Начало периода по часовому поясу UTC.
   * @param to   Конец периода по часовому поясу UTC.
   * @return События выплаты дивидендов по инструменту (если есть).
   */
  @Nonnull
  public CompletableFuture<List<Dividend>> getDividends(@Nonnull String figi,
                                                        @Nonnull Instant from,
                                                        @Nonnull Instant to) {
    ValidationUtils.checkFromTo(from, to);

    return Helpers.<GetDividendsResponse>unaryAsyncCall(
        observer -> instrumentsStub.getDividends(
          GetDividendsRequest.newBuilder()
            .setFigi(figi)
            .setFrom(DateUtils.instantToTimestamp(from))
            .setTo(DateUtils.instantToTimestamp(to))
            .build(),
          observer))
      .thenApply(GetDividendsResponse::getDividendsList);
  }

  private <T> T getInstrumentByTickerSync(@Nonnull String ticker,
                                          @Nonnull String classCode,
                                          Function<InstrumentRequest, T> getter) {
    return Helpers.unaryCall(() -> getter.apply(
      InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId(ticker)
        .setClassCode(classCode)
        .build()));
  }

  private <T> T getInstrumentByFigiSync(@Nonnull String figi,
                                        Function<InstrumentRequest, T> getter) {
    return Helpers.unaryCall(() -> getter.apply(
      InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId(figi)
        .build()));
  }

  private <T, R> CompletableFuture<T> getInstrumentByTicker(@Nonnull String ticker,
                                                            @Nonnull String classCode,
                                                            BiConsumer<InstrumentRequest, StreamObserver<R>> call,
                                                            Function<R, T> extractor) {
    return Helpers.<R>unaryAsyncCall(
        observer -> call.accept(
          InstrumentRequest.newBuilder()
            .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
            .setId(ticker)
            .setClassCode(classCode)
            .build(),
          observer))
      .thenApply(extractor);
  }

  private <T, R> CompletableFuture<T> getInstrumentByFigi(@Nonnull String figi,
                                                          BiConsumer<InstrumentRequest, StreamObserver<R>> call,
                                                          Function<R, T> extractor) {
    return Helpers.<R>unaryAsyncCall(
        observer -> call.accept(
          InstrumentRequest.newBuilder()
            .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
            .setId(figi)
            .build(),
          observer))
      .thenApply(extractor);
  }

  private <T, R> CompletableFuture<T> getInstrumentByPositionUid(@Nonnull String positionUid,
                                                                 BiConsumer<InstrumentRequest, StreamObserver<R>> call,
                                                                 Function<R, T> extractor) {
    return Helpers.<R>unaryAsyncCall(
        observer -> call.accept(
          InstrumentRequest.newBuilder()
            .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_POSITION_UID)
            .setId(positionUid)
            .build(),
          observer))
      .thenApply(extractor);
  }

  private <T> T getInstrumentByPositionUidSync(@Nonnull String positionUid,
                                               Function<InstrumentRequest, T> getter) {
    return Helpers.unaryCall(() -> getter.apply(
      InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_POSITION_UID)
        .setId(positionUid)
        .build()));
  }

  private <T, R> CompletableFuture<T> getInstrumentByUid(@Nonnull String uid,
                                                         BiConsumer<InstrumentRequest, StreamObserver<R>> call,
                                                         Function<R, T> extractor) {
    return Helpers.<R>unaryAsyncCall(
        observer -> call.accept(
          InstrumentRequest.newBuilder()
            .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_UID)
            .setId(uid)
            .build(),
          observer))
      .thenApply(extractor);
  }

  private <T> T getInstrumentByUidSync(@Nonnull String uid,
                                       Function<InstrumentRequest, T> getter) {
    return Helpers.unaryCall(() -> getter.apply(
      InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_UID)
        .setId(uid)
        .build()));
  }

  /**
   * Получение (асинхронное) списка избранных инструментов.
   *
   * @return Список избранных инструментов.
   */
  public CompletableFuture<List<FavoriteInstrument>> getFavorites() {
    return Helpers.<GetFavoritesResponse>unaryAsyncCall(
        observer -> instrumentsStub.getFavorites(
          GetFavoritesRequest.getDefaultInstance(),
          observer))
      .thenApply(GetFavoritesResponse::getFavoriteInstrumentsList);
  }

  /**
   * Получение (синхронное) списка избранных инструментов.
   *
   * @return Список избранных инструментов.
   */
  public List<FavoriteInstrument> getFavoritesSync() {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.getFavorites(GetFavoritesRequest.getDefaultInstance()).getFavoriteInstrumentsList());
  }

  /**
   * Получение (асинхронное) списка стран.
   *
   * @return Список стран.
   */
  public CompletableFuture<List<CountryResponse>> getCountries() {
    return Helpers.<GetCountriesResponse>unaryAsyncCall(
        observer -> instrumentsStub.getCountries(
          GetCountriesRequest.getDefaultInstance(),
          observer))
      .thenApply(GetCountriesResponse::getCountriesList);
  }

  /**
   * Получение (синхронное) списка стран.
   *
   * @return Список стран.
   */
  public List<CountryResponse> getCountriesSync() {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.getCountries(GetCountriesRequest.getDefaultInstance()).getCountriesList());
  }

  /**
   * Получение (асинхронное) списка брендов.
   *
   * @return Список брендов.
   */
  public CompletableFuture<List<Brand>> getBrands() {
    return Helpers.<GetBrandsResponse>unaryAsyncCall(
        observer -> instrumentsStub.getBrands(
          GetBrandsRequest.getDefaultInstance(),
          observer))
      .thenApply(GetBrandsResponse::getBrandsList);
  }

  /**
   * Получение (синхронное) списка брендов.
   *
   * @return Список брендов.
   */
  public List<Brand> getBrandsSync() {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.getBrands(GetBrandsRequest.getDefaultInstance()).getBrandsList());
  }

  /**
   * Получение (асинхронное) списка брендов с пагинацией.
   *
   * @return Список брендов.
   */
  public CompletableFuture<GetBrandsResponse> getBrands(int limit, int page) {
    return Helpers.unaryAsyncCall(
      observer -> instrumentsStub.getBrands(
        GetBrandsRequest.newBuilder()
          .setPaging(Page.newBuilder().setLimit(limit).setPageNumber(page).build())
          .build(),
        observer));
  }

  /**
   * Получение (синхронное) списка брендов с пагинацией.
   *
   * @return Список брендов.
   */
  public GetBrandsResponse getBrandsSync(int limit, int page) {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.getBrands(
      GetBrandsRequest.newBuilder()
        .setPaging(Page.newBuilder().setLimit(limit).setPageNumber(page).build())
        .build()));
  }

  /**
   * Получение (асинхронное) бренда по его идентификатору.
   *
   * @param uid идентификатор бренда.
   * @return Бренд.
   */
  public CompletableFuture<Brand> getBrandBy(String uid) {
    return Helpers.unaryAsyncCall(
      observer -> instrumentsStub.getBrandBy(
        GetBrandRequest.newBuilder().setId(uid).build(),
        observer));
  }

  /**
   * Получение (синхронное) бренда по его идентификатору.
   *
   * @param uid идентификатор бренда.
   * @return Бренд.
   */
  public Brand getBrandBySync(String uid) {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.getBrandBy(GetBrandRequest.newBuilder().setId(uid).build()));
  }

  /**
   * Регистронезависимый поиск (асинхронный) инструмента по одному из его идентификаторов.
   *
   * @param id полный или частичный figi/ticker/isin/uid/name инструмента.
   * @return Бренд.
   */
  public CompletableFuture<List<InstrumentShort>> findInstrument(String id) {
    return Helpers.<FindInstrumentResponse>unaryAsyncCall(
        observer -> instrumentsStub.findInstrument(
          FindInstrumentRequest.newBuilder().setQuery(id).build(),
          observer))
      .thenApply(FindInstrumentResponse::getInstrumentsList);
  }

  /**
   * Регистронезависимый поиск (синхронный) инструмента по одному из его идентификаторов.
   *
   * @param id полный или частичный figi/ticker/isin/uid/name инструмента.
   * @return Бренд.
   */
  public List<InstrumentShort> findInstrumentSync(String id) {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.findInstrument(FindInstrumentRequest.newBuilder().setQuery(id).build()).getInstrumentsList());
  }

  /**
   * Редактирование (асинхронное) списка избранных инструментов.
   *
   * @param figiList   список FIGI инструментов.
   * @param actionType Тип действия со списком избранных инструментов
   * @return Список избранных инструментов.
   */
  public CompletableFuture<List<FavoriteInstrument>> editFavorites(Iterable<String> figiList, EditFavoritesActionType actionType) {
    var builder = EditFavoritesRequest.newBuilder().setActionType(actionType);
    for (String figi : figiList) {
      var instrument = EditFavoritesRequestInstrument.newBuilder()
        .setInstrumentId(figi).build();
      builder.addInstruments(instrument);
    }
    return Helpers.<EditFavoritesResponse>unaryAsyncCall(
        observer -> instrumentsStub.editFavorites(builder.build(), observer))
      .thenApply(EditFavoritesResponse::getFavoriteInstrumentsList);
  }

  /**
   * Добавление (асинхронное) в список избранных инструментов.
   *
   * @param figiList список FIGI инструментов.
   * @return Список избранных инструментов.
   */
  public CompletableFuture<List<FavoriteInstrument>> addFavorites(Iterable<String> figiList) {
    return editFavorites(figiList, EditFavoritesActionType.EDIT_FAVORITES_ACTION_TYPE_ADD);
  }

  /**
   * Удаление (асинхронное) из списка избранных инструментов.
   *
   * @param figiList список FIGI инструментов.
   * @return Список избранных инструментов.
   */
  public CompletableFuture<List<FavoriteInstrument>> deleteFavorites(Iterable<String> figiList) {
    return editFavorites(figiList, EditFavoritesActionType.EDIT_FAVORITES_ACTION_TYPE_DEL);
  }

  /**
   * Редактирование (синхронное) списка избранных инструментов.
   *
   * @param figiList   список FIGI инструментов.
   * @param actionType Тип действия со списком избранных инструментов
   * @return Список избранных инструментов.
   */
  public List<FavoriteInstrument> editFavoritesSync(Iterable<String> figiList, EditFavoritesActionType actionType) {
    var builder = EditFavoritesRequest.newBuilder().setActionType(actionType);
    for (String figi : figiList) {
      var instrument = EditFavoritesRequestInstrument.newBuilder()
        .setInstrumentId(figi).build();
      builder.addInstruments(instrument);
    }
    return Helpers.unaryCall(() -> instrumentsBlockingStub.editFavorites(builder.build()).getFavoriteInstrumentsList());
  }

  /**
   * Добавление (синхронное) в список избранных инструментов.
   *
   * @param figiList список FIGI инструментов.
   * @return Список избранных инструментов.
   */
  public List<FavoriteInstrument> addFavoritesSync(Iterable<String> figiList) {
    return editFavoritesSync(figiList, EditFavoritesActionType.EDIT_FAVORITES_ACTION_TYPE_ADD);
  }

  /**
   * Удаление (синхронное) из списка избранных инструментов.
   *
   * @param figiList список FIGI инструментов.
   * @return Список избранных инструментов.
   */
  public List<FavoriteInstrument> deleteFavoritesSync(Iterable<String> figiList) {
    return editFavoritesSync(figiList, EditFavoritesActionType.EDIT_FAVORITES_ACTION_TYPE_DEL);
  }

  /**
   * Получение графика событий по облигации
   * @param instrumentId figi / instrument_uid инструмента
   * @param from Начало периода по часовому поясу UTC.
   * @param to   Конец периода по часовому поясу UTC.
   * @param eventType Тип события. Возможные значения:
   * EVENT_TYPE_CPN - Купон, EVENT_TYPE_CALL - Опцион (оферта), EVENT_TYPE_MTY - Погашение,
   *                  EVENT_TYPE_CONV - Конвертация
   * @return Список событий по облигации
   */
  public CompletableFuture<List<GetBondEventsResponse.BondEvent>> getBondEvents(
    @Nonnull String instrumentId,
    @Nullable Instant from,
    @Nullable Instant to,
    @Nullable GetBondEventsRequest.EventType eventType) {
    if (from != null) {
      ValidationUtils.checkFromTo(from, to);
    }
    var request = GetBondEventsRequest.newBuilder()
      .setInstrumentId(instrumentId);
    if (from != null) {
      request.setFrom(DateUtils.instantToTimestamp(from));
    }
    if (to != null) {
      request.setTo(DateUtils.instantToTimestamp(to));
    }
    if (eventType != null) {
      request.setType(eventType);
    }
    return Helpers.<GetBondEventsResponse>unaryAsyncCall(
        observer -> instrumentsStub.getBondEvents(request.build(), observer))
      .thenApply(GetBondEventsResponse::getEventsList);
  }

  /**
   * Получение графика событий по облигации
   * @param instrumentId figi / instrument_uid инструмента
   * @param from Начало периода по часовому поясу UTC.
   * @param to   Конец периода по часовому поясу UTC.
   * @return Список событий по облигации
   */
  public CompletableFuture<List<GetBondEventsResponse.BondEvent>> getBondEvents(
    @Nonnull String instrumentId,
    @Nullable Instant from,
    @Nullable Instant to) {
      return getBondEvents(instrumentId, from, to, null);
  }

  /**
   * Получение графика событий по облигации
   * @param instrumentId figi / instrument_uid инструмента
   * @param eventType Тип события. Возможные значения:
   * EVENT_TYPE_CPN - Купон, EVENT_TYPE_CALL - Опцион (оферта), EVENT_TYPE_MTY - Погашение,
   *                  EVENT_TYPE_CONV - Конвертация
   * @return Список событий по облигации
   */
  public CompletableFuture<List<GetBondEventsResponse.BondEvent>> getBondEvents(
    @Nonnull String instrumentId,
    @Nullable GetBondEventsRequest.EventType eventType) {
    return getBondEvents(instrumentId, null, null, eventType);
  }

  /**
   * Получение графика событий по облигации
   * @param instrumentId figi / instrument_uid инструмента
   * @return Список событий по облигации
   */
  public CompletableFuture<List<GetBondEventsResponse.BondEvent>> getBondEvents(
    @Nonnull String instrumentId) {
    return getBondEvents(instrumentId, null, null, null);
  }

  /**
   * Получение (синхронное) графика событий по облигации
   * @param instrumentId figi / instrument_uid инструмента
   * @param from Начало периода по часовому поясу UTC.
   * @param to   Конец периода по часовому поясу UTC.
   * @param eventType Тип события. Возможные значения:
   * EVENT_TYPE_CPN - Купон, EVENT_TYPE_CALL - Опцион (оферта), EVENT_TYPE_MTY - Погашение,
   *                  EVENT_TYPE_CONV - Конвертация
   * @return Список событий по облигации
   */
  public List<GetBondEventsResponse.BondEvent> getBondEventsSync(
    @Nonnull String instrumentId,
    @Nullable Instant from,
    @Nullable Instant to,
    @Nullable GetBondEventsRequest.EventType eventType) {
    if (from != null) {
      ValidationUtils.checkFromTo(from, to);
    }
    var request = GetBondEventsRequest.newBuilder()
      .setInstrumentId(instrumentId);
    if (from != null) {
      request.setFrom(DateUtils.instantToTimestamp(from));
    }
    if (to != null) {
      request.setTo(DateUtils.instantToTimestamp(to));
    }
    if (eventType != null) {
      request.setType(eventType);
    }
    return Helpers.unaryCall(() -> instrumentsBlockingStub
      .getBondEvents(request.build()).getEventsList());
  }

  /**
   * Получение (синхронное) графика событий по облигации
   * @param instrumentId figi / instrument_uid инструмента
   * @param from Начало периода по часовому поясу UTC.
   * @param to   Конец периода по часовому поясу UTC.
   * @return Список событий по облигации
   */
  public List<GetBondEventsResponse.BondEvent> getBondEventsSync(
    @Nonnull String instrumentId,
    @Nullable Instant from,
    @Nullable Instant to
    ) {
    return getBondEventsSync(instrumentId, from, to, null);
  }

  /**
   * Получение (синхронное) графика событий по облигации
   * @param instrumentId figi / instrument_uid инструмента
   * @param eventType Тип события. Возможные значения:
   * EVENT_TYPE_CPN - Купон, EVENT_TYPE_CALL - Опцион (оферта), EVENT_TYPE_MTY - Погашение,
   *                  EVENT_TYPE_CONV - Конвертация
   * @return Список событий по облигации
   */
  public List<GetBondEventsResponse.BondEvent> getBondEventsSync(
    @Nonnull String instrumentId,
    @Nullable GetBondEventsRequest.EventType eventType) {
    return getBondEventsSync(instrumentId, null, null, eventType);
  }

  /**
   * Получение (синхронное) графика событий по облигации
   * @param instrumentId figi / instrument_uid инструмента
   * @return Список событий по облигации
   */
  public List<GetBondEventsResponse.BondEvent> getBondEventsSync(
    @Nonnull String instrumentId) {
    return getBondEventsSync(instrumentId, null, null, null);
  }

  /**
   * Получение информации по отчетностям эмитентов
   *
   * @param instrumentId figi / instrument_uid инструмента
   * @param from         Начало периода по часовому поясу UTC.
   * @param to           Конец периода по часовому поясу UTC.
   * @return Список отчетов эмитента
   */
  public CompletableFuture<List<GetAssetReportsResponse.GetAssetReportsEvent>> getAssetsReports(
    @Nonnull String instrumentId,
    @Nullable Instant from,
    @Nullable Instant to) {
    if (from != null) {
      ValidationUtils.checkFromTo(from, to);
    }
    var request = GetAssetReportsRequest.newBuilder()
      .setInstrumentId(instrumentId);
    if (from != null) {
      request.setFrom(DateUtils.instantToTimestamp(from));
    }
    if (to != null) {
      request.setTo(DateUtils.instantToTimestamp(to));
    }
    return Helpers.<GetAssetReportsResponse>unaryAsyncCall(
        observer -> instrumentsStub.getAssetReports(request.build(), observer))
      .thenApply(GetAssetReportsResponse::getEventsList);
  }

  /**
   * Получение информации по отчетностям эмитентов
   *
   * @param instrumentId figi / instrument_uid инструмента
   * @return Список отчетов эмитента
   */
  public CompletableFuture<List<GetAssetReportsResponse.GetAssetReportsEvent>> getAssetsReports(
    @Nonnull String instrumentId) {
    return getAssetsReports(instrumentId, null, null);
  }

  /**
   * Получение (синхронное) информации по отчетностям эмитентов
   *
   * @param instrumentId figi / instrument_uid инструмента
   * @param from         Начало периода по часовому поясу UTC.
   * @param to           Конец периода по часовому поясу UTC.
   * @return Список отчетов эмитента
   */
  public List<GetAssetReportsResponse.GetAssetReportsEvent> getAssetsReportsSync(
    @Nonnull String instrumentId,
    @Nullable Instant from,
    @Nullable Instant to) {
    if (from != null) {
      ValidationUtils.checkFromTo(from, to);
    }
    var request = GetAssetReportsRequest.newBuilder()
      .setInstrumentId(instrumentId);
    if (from != null) {
      request.setFrom(DateUtils.instantToTimestamp(from));
    }
    if (to != null) {
      request.setTo(DateUtils.instantToTimestamp(to));
    }
    return Helpers.unaryCall(() -> instrumentsBlockingStub
      .getAssetReports(request.build()).getEventsList());
  }

  /**
   * Получение (синхронное) информации по отчетностям эмитентов
   *
   * @param instrumentId figi / instrument_uid инструмента
   * @return Список отчетов эмитента
   */
  public List<GetAssetReportsResponse.GetAssetReportsEvent> getAssetsReportsSync(
    @Nonnull String instrumentId) {
    return getAssetsReportsSync(instrumentId, null, null);
  }

  /**
   * Получение информации по прогнозам на инструмент
   *
   * @param limit         Максимальное число возвращаемых записей.
   * @param page          Порядковый номер страницы, начиная с 0.
   * @return список прогнозов с данными пагинации
   */
  public CompletableFuture<GetConsensusForecastsResponse> getConsensusForecasts(
    @Nullable Integer limit,
    @Nullable Integer page) {
    var paging = Page.newBuilder();
    if (limit != null) {
      paging.setLimit(limit);
    }
    if (page != null) {
      paging.setPageNumber(page);
    }
    var request = GetConsensusForecastsRequest.newBuilder().setPaging(paging).build();
    return Helpers.unaryAsyncCall(
        observer -> instrumentsStub.getConsensusForecasts(request, observer));
  }

  /**
   * Получение информации по прогнозам на инструмент
   *
   * @return список прогнозов с данными пагинации
   */
  public CompletableFuture<GetConsensusForecastsResponse> getConsensusForecasts() {
    var request = GetConsensusForecastsRequest.newBuilder().build();
    return Helpers.unaryAsyncCall(
      observer -> instrumentsStub.getConsensusForecasts(request, observer));
  }

  /**
   * Получение (синхронное) информации по прогнозам на инструмент
   *
   * @param limit         Максимальное число возвращаемых записей.
   * @param page          Порядковый номер страницы, начиная с 0.
   * @return список прогнозов с данными пагинации
   */
  public GetConsensusForecastsResponse getConsensusForecastsSync(
    @Nullable Integer limit,
    @Nullable Integer page) {
    var paging = Page.newBuilder();
    if (limit != null) {
      paging.setLimit(limit);
    }
    if (page != null) {
      paging.setPageNumber(page);
    }
    var request = GetConsensusForecastsRequest.newBuilder().setPaging(paging).build();
    return Helpers.unaryCall(() -> instrumentsBlockingStub
      .getConsensusForecasts(request));
  }

  /**
   * Получение (синхронное) информации по прогнозам на инструмент
   *
   * @return список прогнозов с данными пагинации
   */
  public GetConsensusForecastsResponse getConsensusForecastsSync() {
    var request = GetConsensusForecastsRequest.newBuilder().build();
    return Helpers.unaryCall(() -> instrumentsBlockingStub
      .getConsensusForecasts(request));
  }

  /**
   * Получение прогнозов по идентификатору инструмента
   *
   * @param instrumentId figi / instrument_uid инструмента
   * @return Список прогнозов
   */
  public CompletableFuture<GetForecastResponse> getForecastBy(@Nonnull String instrumentId) {
    var request = GetForecastRequest.newBuilder().setInstrumentId(instrumentId).build();
    return Helpers.unaryAsyncCall(observer -> instrumentsStub.getForecastBy(request, observer));
  }

  /**
   * Получение (синхронное) прогнозов по идентификатору инструмента
   *
   * @param instrumentId figi / instrument_uid инструмента
   * @return Список прогнозов
   */
  public GetForecastResponse getForecastBySync(@Nonnull String instrumentId) {
    var request = GetForecastRequest.newBuilder().setInstrumentId(instrumentId).build();
    return Helpers.unaryCall(() -> instrumentsBlockingStub.getForecastBy(request));
  }

  /**
   * Получение списка всех индикативных инструментов (индексов, товаров и др.).
   *
   * @return Список индикативных инструментов.
   */
  @Nonnull
  public CompletableFuture<List<IndicativeResponse>> getIndicatives() {
    return Helpers.<IndicativesResponse>unaryAsyncCall(
        observer -> instrumentsStub.indicatives(
          IndicativesRequest.newBuilder().build(),
          observer))
      .thenApply(IndicativesResponse::getInstrumentsList);
  }

  /**
   * Получение (синхронное) списка всех индикативных инструментов (индексов, товаров и др.).
   *
   * @return Список индикативных инструментов.
   */
  @Nonnull
  public List<IndicativeResponse> getIndicativesSync() {
    return Helpers.unaryCall(() -> instrumentsBlockingStub.indicatives(IndicativesRequest.newBuilder().build())
      .getInstrumentsList());
  }

  /**
   * Фундаментальные показатели по активу
   *
   * @param assetIds
   * @return
   */
  public CompletableFuture<GetAssetFundamentalsResponse> getAssetFundamentals(Collection<String> assetIds) {
    return Helpers.unaryAsyncCall(
      observer -> instrumentsStub.getAssetFundamentals(
        GetAssetFundamentalsRequest.newBuilder()
          .addAllAssets(assetIds)
          .build(),
        observer
      ));
  }

  /**
   * Фундаментальные показатели по активу
   *
   * @param assetIds
   * @return
   */
  public GetAssetFundamentalsResponse getAssetFundamentalsSync(Collection<String> assetIds) {
    return Helpers.unaryCall(
      () -> instrumentsBlockingStub.getAssetFundamentals(
        GetAssetFundamentalsRequest.newBuilder()
          .addAllAssets(assetIds)
          .build()
      ));
  }

}
