package ru.tinkoff.piapi.core;

import com.google.protobuf.Timestamp;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.rules.ExpectedException;
import ru.tinkoff.piapi.contract.v1.AccruedInterest;
import ru.tinkoff.piapi.contract.v1.Asset;
import ru.tinkoff.piapi.contract.v1.AssetFull;
import ru.tinkoff.piapi.contract.v1.AssetRequest;
import ru.tinkoff.piapi.contract.v1.AssetResponse;
import ru.tinkoff.piapi.contract.v1.AssetType;
import ru.tinkoff.piapi.contract.v1.AssetsRequest;
import ru.tinkoff.piapi.contract.v1.AssetsResponse;
import ru.tinkoff.piapi.contract.v1.Bond;
import ru.tinkoff.piapi.contract.v1.BondResponse;
import ru.tinkoff.piapi.contract.v1.BondsResponse;
import ru.tinkoff.piapi.contract.v1.Brand;
import ru.tinkoff.piapi.contract.v1.CountryResponse;
import ru.tinkoff.piapi.contract.v1.CurrenciesResponse;
import ru.tinkoff.piapi.contract.v1.Currency;
import ru.tinkoff.piapi.contract.v1.CurrencyResponse;
import ru.tinkoff.piapi.contract.v1.Dividend;
import ru.tinkoff.piapi.contract.v1.EditFavoritesRequest;
import ru.tinkoff.piapi.contract.v1.EditFavoritesResponse;
import ru.tinkoff.piapi.contract.v1.Etf;
import ru.tinkoff.piapi.contract.v1.EtfResponse;
import ru.tinkoff.piapi.contract.v1.EtfsResponse;
import ru.tinkoff.piapi.contract.v1.FavoriteInstrument;
import ru.tinkoff.piapi.contract.v1.FindInstrumentRequest;
import ru.tinkoff.piapi.contract.v1.FindInstrumentResponse;
import ru.tinkoff.piapi.contract.v1.Future;
import ru.tinkoff.piapi.contract.v1.FutureResponse;
import ru.tinkoff.piapi.contract.v1.FuturesResponse;
import ru.tinkoff.piapi.contract.v1.GetAccruedInterestsRequest;
import ru.tinkoff.piapi.contract.v1.GetAccruedInterestsResponse;
import ru.tinkoff.piapi.contract.v1.GetBrandRequest;
import ru.tinkoff.piapi.contract.v1.GetBrandsRequest;
import ru.tinkoff.piapi.contract.v1.GetBrandsResponse;
import ru.tinkoff.piapi.contract.v1.GetCountriesRequest;
import ru.tinkoff.piapi.contract.v1.GetCountriesResponse;
import ru.tinkoff.piapi.contract.v1.GetDividendsRequest;
import ru.tinkoff.piapi.contract.v1.GetDividendsResponse;
import ru.tinkoff.piapi.contract.v1.GetFavoritesRequest;
import ru.tinkoff.piapi.contract.v1.GetFavoritesResponse;
import ru.tinkoff.piapi.contract.v1.GetFuturesMarginRequest;
import ru.tinkoff.piapi.contract.v1.GetFuturesMarginResponse;
import ru.tinkoff.piapi.contract.v1.Instrument;
import ru.tinkoff.piapi.contract.v1.InstrumentIdType;
import ru.tinkoff.piapi.contract.v1.InstrumentRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentResponse;
import ru.tinkoff.piapi.contract.v1.InstrumentShort;
import ru.tinkoff.piapi.contract.v1.InstrumentStatus;
import ru.tinkoff.piapi.contract.v1.InstrumentsRequest;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.contract.v1.Share;
import ru.tinkoff.piapi.contract.v1.ShareResponse;
import ru.tinkoff.piapi.contract.v1.SharesResponse;
import ru.tinkoff.piapi.contract.v1.TradingSchedule;
import ru.tinkoff.piapi.contract.v1.TradingSchedulesRequest;
import ru.tinkoff.piapi.contract.v1.TradingSchedulesResponse;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.tinkoff.piapi.core.utils.DateUtils.timestampToInstant;

public class InstrumentsServiceTest extends GrpcClientTester<InstrumentsService> {

  @Rule
  public ExpectedException futureThrown = ExpectedException.none();

  @Override
  protected InstrumentsService createClient(Channel channel) {
    return new InstrumentsService(
      InstrumentsServiceGrpc.newBlockingStub(channel),
      InstrumentsServiceGrpc.newStub(channel));
  }

  private void assertThrowsApiRuntimeException(String code, Executable executable) {
    var apiRuntimeException = assertThrows(ApiRuntimeException.class, executable);
    assertEquals(code, apiRuntimeException.getCode());
  }

  private void assertThrowsAsyncApiRuntimeException(String code, Executable executable) {
    var throwable = assertThrows(CompletionException.class, executable).getCause();
    assertTrue(throwable instanceof ApiRuntimeException);
    assertEquals(code, ((ApiRuntimeException) throwable).getCode());
  }

  @Nested
  class GetCountriesTest {

    @Test
    void getCountriesTest() {
      var country = CountryResponse.newBuilder().setAlfaTwo("EN").setAlfaThree("ENG").build();
      var expected = GetCountriesResponse.newBuilder().addCountries(country).build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getCountries(GetCountriesRequest request, StreamObserver<GetCountriesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var actualSync = service.getCountriesSync();
      var actualAsync = service.getCountries().join();

      assertIterableEquals(expected.getCountriesList(), actualSync);
      assertIterableEquals(expected.getCountriesList(), actualAsync);

      verify(grpcService, times(2)).getCountries(any(), any());
    }
  }

  @Nested
  class FindInstrumentTest {

    @Test
    void findInstrumentTest() {
      var instrument = InstrumentShort.newBuilder().setFigi("figi").build();
      var expected = FindInstrumentResponse.newBuilder().addInstruments(instrument).build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void findInstrument(FindInstrumentRequest request, StreamObserver<FindInstrumentResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var actualSync = service.findInstrumentSync("my_id");
      var actualAsync = service.findInstrument("my_id").join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).findInstrument(any(), any());
    }
  }

  @Nested
  class GetBrandsTest {

    @Test
    void getBrandsTest() {
      var brand = Brand.newBuilder().setCompany("company").setCountryOfRisk("risk").build();
      var expected = GetBrandsResponse.newBuilder().addBrands(brand).build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getBrands(GetBrandsRequest request, StreamObserver<GetBrandsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var actualSync = service.getBrandsSync();
      var actualAsync = service.getBrands().join();

      assertIterableEquals(expected.getBrandsList(), actualSync);
      assertIterableEquals(expected.getBrandsList(), actualAsync);

      verify(grpcService, times(2)).getBrands(any(), any());
    }

    @Test
    void getBrandByTest() {
      var id = "my_id";
      var brand = Brand.newBuilder().setCompany("company").setCountryOfRisk("risk").build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getBrandBy(GetBrandRequest request, StreamObserver<Brand> responseObserver) {
            responseObserver.onNext(brand);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var actualSync = service.getBrandBySync(id);
      var actualAsync = service.getBrandBy(id).join();

      assertEquals(brand, actualSync);
      assertEquals(brand, actualAsync);

      verify(grpcService, times(2)).getBrandBy(any(), any());
    }
  }

  @Nested
  class FavoritesTest {

    @Test
    void getFavoritesTest() {
      var instrument = FavoriteInstrument.newBuilder().setFigi("fav_figi").build();
      var expected = GetFavoritesResponse.newBuilder().addFavoriteInstruments(instrument).build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getFavorites(GetFavoritesRequest request, StreamObserver<GetFavoritesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var actualSync = service.getFavoritesSync();
      var actualAsync = service.getFavorites().join();

      assertIterableEquals(expected.getFavoriteInstrumentsList(), actualSync);
      assertIterableEquals(expected.getFavoriteInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).getFavorites(any(), any());
    }

    @Test
    void addFavoritesTest() {
      var instrument = FavoriteInstrument.newBuilder().setFigi("fav_figi").build();
      var expected = EditFavoritesResponse
        .newBuilder()
        .addFavoriteInstruments(instrument)
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void editFavorites(EditFavoritesRequest request, StreamObserver<EditFavoritesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var actualSync = service.addFavoritesSync(List.of("fav_figi"));
      var actualAsync = service.addFavorites(List.of("fav_figi")).join();

      assertIterableEquals(expected.getFavoriteInstrumentsList(), actualSync);
      assertIterableEquals(expected.getFavoriteInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).editFavorites(any(), any());
    }

    @Test
    void delFavoritesTest() {
      var instrument = FavoriteInstrument.newBuilder().setFigi("fav_figi").build();
      var expected = EditFavoritesResponse
        .newBuilder()
        .addFavoriteInstruments(instrument)
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void editFavorites(EditFavoritesRequest request, StreamObserver<EditFavoritesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var actualSync = service.deleteFavoritesSync(List.of("fav_figi"));
      var actualAsync = service.deleteFavorites(List.of("fav_figi")).join();

      assertIterableEquals(expected.getFavoriteInstrumentsList(), actualSync);
      assertIterableEquals(expected.getFavoriteInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).editFavorites(any(), any());
    }

  }
  @Nested
  class GetTradingSchedulesTest {

    @Test
    void getAllSchedules_Test() {
      var expected = TradingSchedulesResponse.newBuilder()
        .addExchanges(TradingSchedule.newBuilder().setExchange("MOEX").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void tradingSchedules(TradingSchedulesRequest request,
                                       StreamObserver<TradingSchedulesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = TradingSchedulesRequest.newBuilder()
        .setFrom(Timestamp.newBuilder().setSeconds(1234567890).build())
        .setTo(Timestamp.newBuilder().setSeconds(1234567890).setNanos(111222333).build())
        .build();
      var actualSync = service.getTradingSchedulesSync(
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo()));
      var actualAsync = service.getTradingSchedules(
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())).join();

      assertIterableEquals(expected.getExchangesList(), actualSync);
      assertIterableEquals(expected.getExchangesList(), actualAsync);

      verify(grpcService, times(2)).tradingSchedules(eq(inArg), any());
    }

    @Test
    void getAllSchedules_shouldThrowIfToIsNotAfterFrom_Test() {
      var expected = TradingSchedulesResponse.newBuilder()
        .addExchanges(TradingSchedule.newBuilder().setExchange("MOEX").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void tradingSchedules(TradingSchedulesRequest request,
                                       StreamObserver<TradingSchedulesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var now = Instant.now();
      var nowMinusSecond = now.minusSeconds(1);
      assertThrows(IllegalArgumentException.class, () -> service.getTradingSchedulesSync(now, nowMinusSecond));
      futureThrown.expect(CompletionException.class);
      futureThrown.expectCause(IsInstanceOf.instanceOf(IllegalArgumentException.class));
      assertThrows(IllegalArgumentException.class, () -> service.getTradingSchedules(now, nowMinusSecond));

      verify(grpcService, never()).tradingSchedules(any(), any());
    }

    @Test
    void getOneSchedule_Test() {
      var exchange = "MOEX";
      var expected = TradingSchedulesResponse.newBuilder()
        .addExchanges(TradingSchedule.newBuilder().setExchange(exchange).build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void tradingSchedules(TradingSchedulesRequest request,
                                       StreamObserver<TradingSchedulesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = TradingSchedulesRequest.newBuilder()
        .setExchange(exchange)
        .setFrom(Timestamp.newBuilder().setSeconds(1234567890).build())
        .setTo(Timestamp.newBuilder().setSeconds(1234567890).setNanos(111222333).build())
        .build();
      var actualSync = service.getTradingScheduleSync(
        exchange,
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo()));
      var actualAsync = service.getTradingSchedule(
        exchange,
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())).join();

      assertEquals(expected.getExchangesList().get(0), actualSync);
      assertEquals(expected.getExchangesList().get(0), actualAsync);

      verify(grpcService, times(2)).tradingSchedules(eq(inArg), any());
    }

    @Test
    void getOneSchedule_shouldThrowIfToIsNotAfterFrom_Test() {
      var exchange = "MOEX";
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
        }));
      var service = mkClientBasedOnServer(grpcService);

      var now = Instant.now();
      var nowMinusSecond = now.minusSeconds(1);
      assertThrows(IllegalArgumentException.class, () -> service.getTradingScheduleSync(exchange, now, nowMinusSecond));
      futureThrown.expect(CompletionException.class);
      futureThrown.expectCause(IsInstanceOf.instanceOf(IllegalArgumentException.class));
      assertThrows(IllegalArgumentException.class, () -> service.getTradingSchedule(exchange, now, nowMinusSecond));

      verify(grpcService, never()).tradingSchedules(any(), any());
    }

    @Test
    void getOneSchedule_shouldReturnNoneInCaseOfNotFoundStatus_Test() {
      var exchange = "MOEX";
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void tradingSchedules(TradingSchedulesRequest request,
                                       StreamObserver<TradingSchedulesResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50001")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = TradingSchedulesRequest.newBuilder()
        .setExchange(exchange)
        .setFrom(Timestamp.newBuilder().setSeconds(1234567890).build())
        .setTo(Timestamp.newBuilder().setSeconds(1234567890).setNanos(111222333).build())
        .build();
      assertThrowsApiRuntimeException("50001", () -> service.getTradingScheduleSync(
        exchange,
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())));
      assertThrowsAsyncApiRuntimeException("50001", () -> service.getTradingSchedule(
        exchange,
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())).join());


      verify(grpcService, times(2)).tradingSchedules(eq(inArg), any());
    }

  }

  @Nested
  class GetBondsTest {

    @Test
    void getOneByTicker_Test() {
      var expected = BondResponse.newBuilder()
        .setInstrument(Bond.newBuilder().setTicker("TCS").setClassCode("moex").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void bondBy(InstrumentRequest request,
                             StreamObserver<BondResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();
      var actualSync = service.getBondByTickerSync(inArg.getId(), inArg.getClassCode());
      var actualAsync = service.getBondByTicker(inArg.getId(), inArg.getClassCode()).join();

      assertEquals(expected.getInstrument(), actualSync);
      assertEquals(expected.getInstrument(), actualAsync);

      verify(grpcService, times(2)).bondBy(eq(inArg), any());
    }

    @Test
    void getOneByTicker_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void bondBy(InstrumentRequest request,
                             StreamObserver<BondResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getBondByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getBondByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).bondBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_Test() {
      var expected = BondResponse.newBuilder()
        .setInstrument(Bond.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void bondBy(InstrumentRequest request,
                             StreamObserver<BondResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();
      var actualSync = service.getBondByFigiSync(inArg.getId());
      var actualAsync = service.getBondByFigi(inArg.getId()).join();

      assertEquals(expected.getInstrument(), actualSync);
      assertEquals(expected.getInstrument(), actualAsync);

      verify(grpcService, times(2)).bondBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void bondBy(InstrumentRequest request,
                             StreamObserver<BondResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getBondByFigiSync(inArg.getId()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getBondByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).bondBy(eq(inArg), any());
    }

    @Test
    void getTradable_Test() {
      var expected = BondsResponse.newBuilder()
        .addInstruments(Bond.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void bonds(InstrumentsRequest request,
                            StreamObserver<BondsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
        .build();
      var actualSync = service.getTradableBondsSync();
      var actualAsync = service.getTradableBonds().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).bonds(eq(inArg), any());
    }

    @Test
    void getAll_Test() {
      var expected = BondsResponse.newBuilder()
        .addInstruments(Bond.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void bonds(InstrumentsRequest request,
                            StreamObserver<BondsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
        .build();
      var actualSync = service.getAllBondsSync();
      var actualAsync = service.getAllBonds().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).bonds(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_all_Test() {
      var expected = BondsResponse.newBuilder()
        .addInstruments(Bond.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void bonds(InstrumentsRequest request,
                            StreamObserver<BondsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_ALL;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getBondsSync(instrumentStatus);
      var actualAsync = service.getBonds(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).bonds(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_base_Test() {
      var expected = BondsResponse.newBuilder()
        .addInstruments(Bond.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void bonds(InstrumentsRequest request,
                            StreamObserver<BondsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_BASE;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getBondsSync(instrumentStatus);
      var actualAsync = service.getBonds(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).bonds(eq(inArg), any());
    }

  }

  @Nested
  class GetCurrenciesTest {

    @Test
    void getOneByTicker_Test() {
      var expected = CurrencyResponse.newBuilder()
        .setInstrument(Currency.newBuilder().setTicker("TCS").setClassCode("moex").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void currencyBy(InstrumentRequest request,
                                 StreamObserver<CurrencyResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();
      var actualSync = service.getCurrencyByTickerSync(inArg.getId(), inArg.getClassCode());
      var actualAsync = service.getCurrencyByTicker(inArg.getId(), inArg.getClassCode()).join();

      assertEquals(expected.getInstrument(), actualSync);
      assertEquals(expected.getInstrument(), actualAsync);

      verify(grpcService, times(2)).currencyBy(eq(inArg), any());
    }

    @Test
    void getOneByTicker_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void currencyBy(InstrumentRequest request,
                                 StreamObserver<CurrencyResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getCurrencyByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getCurrencyByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).currencyBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_Test() {
      var expected = CurrencyResponse.newBuilder()
        .setInstrument(Currency.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void currencyBy(InstrumentRequest request,
                                 StreamObserver<CurrencyResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();
      var actualSync = service.getCurrencyByFigiSync(inArg.getId());
      var actualAsync = service.getCurrencyByFigi(inArg.getId()).join();

      assertEquals(expected.getInstrument(), actualSync);
      assertEquals(expected.getInstrument(), actualAsync);

      verify(grpcService, times(2)).currencyBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void currencyBy(InstrumentRequest request,
                                 StreamObserver<CurrencyResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getCurrencyByFigiSync(inArg.getId()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getCurrencyByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).currencyBy(eq(inArg), any());
    }

    @Test
    void getTradable_Test() {
      var expected = CurrenciesResponse.newBuilder()
        .addInstruments(Currency.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void currencies(InstrumentsRequest request,
                                 StreamObserver<CurrenciesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
        .build();
      var actualSync = service.getTradableCurrenciesSync();
      var actualAsync = service.getTradableCurrencies().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).currencies(eq(inArg), any());
    }

    @Test
    void getAll_Test() {
      var expected = CurrenciesResponse.newBuilder()
        .addInstruments(Currency.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void currencies(InstrumentsRequest request,
                                 StreamObserver<CurrenciesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
        .build();
      var actualSync = service.getAllCurrenciesSync();
      var actualAsync = service.getAllCurrencies().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).currencies(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_all_Test() {
      var expected = CurrenciesResponse.newBuilder()
        .addInstruments(Currency.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void currencies(InstrumentsRequest request,
                                 StreamObserver<CurrenciesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_ALL;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getCurrenciesSync(instrumentStatus);
      var actualAsync = service.getCurrencies(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).currencies(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_base_Test() {
      var expected = CurrenciesResponse.newBuilder()
        .addInstruments(Currency.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void currencies(InstrumentsRequest request,
                                 StreamObserver<CurrenciesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_BASE;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getCurrenciesSync(instrumentStatus);
      var actualAsync = service.getCurrencies(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).currencies(eq(inArg), any());
    }

  }

  @Nested
  class GetEtfsTest {

    @Test
    void getOneByTicker_Test() {
      var expected = EtfResponse.newBuilder()
        .setInstrument(Etf.newBuilder().setTicker("TCS").setClassCode("moex").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void etfBy(InstrumentRequest request,
                            StreamObserver<EtfResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();

      Assertions.assertEquals(expected.getInstrument(), service.getEtfByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertEquals(expected.getInstrument(), service.getEtfByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).etfBy(eq(inArg), any());
    }

    @Test
    void getOneByTicker_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void etfBy(InstrumentRequest request,
                            StreamObserver<EtfResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getEtfByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getEtfByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).etfBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_Test() {
      var expected = EtfResponse.newBuilder()
        .setInstrument(Etf.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void etfBy(InstrumentRequest request,
                            StreamObserver<EtfResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertEquals(expected.getInstrument(), service.getEtfByFigiSync(inArg.getId()));
      assertEquals(expected.getInstrument(), service.getEtfByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).etfBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void etfBy(InstrumentRequest request,
                            StreamObserver<EtfResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getEtfByFigiSync(inArg.getId()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getEtfByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).etfBy(eq(inArg), any());
    }

    @Test
    void getTradable_Test() {
      var expected = EtfsResponse.newBuilder()
        .addInstruments(Etf.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void etfs(InstrumentsRequest request,
                           StreamObserver<EtfsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
        .build();
      var actualSync = service.getTradableEtfsSync();
      var actualAsync = service.getTradableEtfs().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).etfs(eq(inArg), any());
    }

    @Test
    void getAll_Test() {
      var expected = EtfsResponse.newBuilder()
        .addInstruments(Etf.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void etfs(InstrumentsRequest request,
                           StreamObserver<EtfsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
        .build();
      var actualSync = service.getAllEtfsSync();
      var actualAsync = service.getAllEtfs().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).etfs(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_all_test() {
      var expected = EtfsResponse.newBuilder()
        .addInstruments(Etf.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void etfs(InstrumentsRequest request,
                           StreamObserver<EtfsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_ALL;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getEtfsSync(instrumentStatus);
      var actualAsync = service.getEtfs(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).etfs(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_base_test() {
      var expected = EtfsResponse.newBuilder()
        .addInstruments(Etf.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void etfs(InstrumentsRequest request,
                           StreamObserver<EtfsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_BASE;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getEtfsSync(instrumentStatus);
      var actualAsync = service.getEtfs(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).etfs(eq(inArg), any());
    }

  }

  @Nested
  class GetFuturesTest {

    @Test
    void getOneByTicker_Test() {
      var expected = FutureResponse.newBuilder()
        .setInstrument(Future.newBuilder().setTicker("TCS").setClassCode("moex").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void futureBy(InstrumentRequest request,
                               StreamObserver<FutureResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();

      assertEquals(expected.getInstrument(), service.getFutureByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertEquals(expected.getInstrument(), service.getFutureByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).futureBy(eq(inArg), any());
    }

    @Test
    void getOneByTicker_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void futureBy(InstrumentRequest request,
                               StreamObserver<FutureResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("ticker")
        .setClassCode("MOEX")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getFutureByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getFutureByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).futureBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_Test() {
      var expected = FutureResponse.newBuilder()
        .setInstrument(Future.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void futureBy(InstrumentRequest request,
                               StreamObserver<FutureResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertEquals(expected.getInstrument(), service.getFutureByFigiSync(inArg.getId()));
      assertEquals(expected.getInstrument(), service.getFutureByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).futureBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void futureBy(InstrumentRequest request,
                               StreamObserver<FutureResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getFutureByFigiSync(inArg.getId()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getFutureByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).futureBy(eq(inArg), any());
    }

    @Test
    void getTradable_Test() {
      var expected = FuturesResponse.newBuilder()
        .addInstruments(Future.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void futures(InstrumentsRequest request,
                              StreamObserver<FuturesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
        .build();
      var actualSync = service.getTradableFuturesSync();
      var actualAsync = service.getTradableFutures().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).futures(eq(inArg), any());
    }

    @Test
    void getAll_Test() {
      var expected = FuturesResponse.newBuilder()
        .addInstruments(Future.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void futures(InstrumentsRequest request,
                              StreamObserver<FuturesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
        .build();
      var actualSync = service.getAllFuturesSync();
      var actualAsync = service.getAllFutures().join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).futures(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_all_Test() {
      var expected = FuturesResponse.newBuilder()
        .addInstruments(Future.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void futures(InstrumentsRequest request,
                              StreamObserver<FuturesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_ALL;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getFuturesSync(instrumentStatus);
      var actualAsync = service.getFutures(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).futures(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_base_Test() {
      var expected = FuturesResponse.newBuilder()
        .addInstruments(Future.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void futures(InstrumentsRequest request,
                              StreamObserver<FuturesResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_BASE;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();
      var actualSync = service.getFuturesSync(instrumentStatus);
      var actualAsync = service.getFutures(instrumentStatus).join();

      assertIterableEquals(expected.getInstrumentsList(), actualSync);
      assertIterableEquals(expected.getInstrumentsList(), actualAsync);

      verify(grpcService, times(2)).futures(eq(inArg), any());
    }

  }

  @Nested
  class GetSharesTest {

    ShareResponse expectedShareResponse;
    SharesResponse expectedSharesResponse;
    InstrumentsServiceGrpc.InstrumentsServiceImplBase grpcService;
    InstrumentsService service;

    String ticker = UUID.randomUUID().toString();
    String tickerUnknown = UUID.randomUUID().toString();
    String uid = UUID.randomUUID().toString();
    String uidUnknown = UUID.randomUUID().toString();
    String positionUid = UUID.randomUUID().toString();
    String positionUidUnknown = UUID.randomUUID().toString();
    String figi = UUID.randomUUID().toString();
    String figiUnknown = UUID.randomUUID().toString();

    {
      Share expectedShare = Share.newBuilder()
        .setTicker(ticker)
        .setFigi(figi)
        .setUid(uid)
        .setPositionUid(positionUid)
        .setClassCode("moex").build();
      expectedShareResponse = ShareResponse.newBuilder()
        .setInstrument(expectedShare).build();
      expectedSharesResponse = SharesResponse.newBuilder()
        .addInstruments(expectedShare).build();


      grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void shareBy(InstrumentRequest request,
                              StreamObserver<ShareResponse> responseObserver) {
            if (request.getId().equals(tickerUnknown) || request.getId().equals(figiUnknown)
              || request.getId().equals(uidUnknown) || request.getId().equals(positionUidUnknown)) {
              responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
              return;
            }
            responseObserver.onNext(expectedShareResponse);
            responseObserver.onCompleted();
          }
          @Override
          public void shares(InstrumentsRequest request,
                             StreamObserver<SharesResponse> responseObserver) {
            responseObserver.onNext(expectedSharesResponse);
            responseObserver.onCompleted();
          }
        }));

      service = mkClientBasedOnServer(grpcService);
    }


    @Test
    void getOneByTicker_Test() {
      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId(expectedShareResponse.getInstrument().getTicker())
        .setClassCode("moex")
        .build();

      assertEquals(expectedShareResponse.getInstrument(), service.getShareByTickerSync(inArg.getId(), inArg.getClassCode()));
      verify(grpcService, times(1)).shareBy(eq(inArg), any());
      assertEquals(expectedShareResponse.getInstrument(), service.getShareByTicker(inArg.getId(), inArg.getClassCode()).join());
      verify(grpcService, times(2)).shareBy(eq(inArg), any());
    }

    @Test
    void getOneByTicker_shouldReturnEmptyInCaseOfNotFound_Test() {
      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId(tickerUnknown)
        .setClassCode("MOEX")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getShareByTickerSync(inArg.getId(), inArg.getClassCode()));
      verify(grpcService, times(1)).shareBy(eq(inArg), any());
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getShareByTicker(inArg.getId(), inArg.getClassCode()).join());
      verify(grpcService, times(2)).shareBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_Test() {
      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId(expectedShareResponse.getInstrument().getFigi())
        .build();

      assertEquals(expectedShareResponse.getInstrument(), service.getShareByFigiSync(inArg.getId()));
      verify(grpcService, times(1)).shareBy(eq(inArg), any());
      assertEquals(expectedShareResponse.getInstrument(), service.getShareByFigi(inArg.getId()).join());
      verify(grpcService, times(2)).shareBy(eq(inArg), any());
    }

    @Test
    void getOneByFigi_shouldReturnEmptyInCaseOfNotFound_Test() {
      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId(figiUnknown)
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getShareByFigiSync(inArg.getId()));
      verify(grpcService, times(1)).shareBy(eq(inArg), any());
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getShareByFigi(inArg.getId()).join());
      verify(grpcService, times(2)).shareBy(eq(inArg), any());
    }

    @Test
    void getOneByUid_Test() {
      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_UID)
        .setId(expectedShareResponse.getInstrument().getUid())
        .build();

      assertEquals(expectedShareResponse.getInstrument(), service.getShareByUidSync(inArg.getId()));
      verify(grpcService, times(1)).shareBy(eq(inArg), any());
      assertEquals(expectedShareResponse.getInstrument(), service.getShareByUid(inArg.getId()).join());
      verify(grpcService, times(2)).shareBy(eq(inArg), any());
    }

    @Test
    void getOneByUid_shouldReturnEmptyInCaseOfNotFound_Test() {
      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_UID)
        .setId(uidUnknown)
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getShareByUidSync(inArg.getId()));
      verify(grpcService, times(1)).shareBy(eq(inArg), any());
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getShareByUid(inArg.getId()).join());
      verify(grpcService, times(2)).shareBy(eq(inArg), any());
    }

    @Test
    void getOneByPositionUid_Test() {
      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_POSITION_UID)
        .setId(expectedShareResponse.getInstrument().getPositionUid())
        .build();

      assertEquals(expectedShareResponse.getInstrument(), service.getShareByPositionUidSync(inArg.getId()));
      verify(grpcService, times(1)).shareBy(eq(inArg), any());
      assertEquals(expectedShareResponse.getInstrument(), service.getShareByPositionUid(inArg.getId()).join());
      verify(grpcService, times(2)).shareBy(eq(inArg), any());
    }

    @Test
    void getOneByPositionUid_shouldReturnEmptyInCaseOfNotFound_Test() {
      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_POSITION_UID)
        .setId(positionUidUnknown)
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getShareByPositionUidSync(inArg.getId()));
      verify(grpcService, times(1)).shareBy(eq(inArg), any());
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getShareByPositionUid(inArg.getId()).join());
      verify(grpcService, times(2)).shareBy(eq(inArg), any());
    }

    @Test
    void getTradable_Test() {
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_BASE)
        .build();

      assertIterableEquals(expectedSharesResponse.getInstrumentsList(), service.getTradableSharesSync());
      verify(grpcService, times(1)).shares(eq(inArg), any());
      assertIterableEquals(expectedSharesResponse.getInstrumentsList(), service.getTradableShares().join());
      verify(grpcService, times(2)).shares(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_all_Test() {
      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_ALL;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();

      assertIterableEquals(expectedSharesResponse.getInstrumentsList(), service.getSharesSync(instrumentStatus));
      verify(grpcService, times(1)).shares(eq(inArg), any());
      assertIterableEquals(expectedSharesResponse.getInstrumentsList(), service.getShares(instrumentStatus).join());
      verify(grpcService, times(2)).shares(eq(inArg), any());
    }

    @Test
    void getByInstrumentStatus_base_Test() {
      var instrumentStatus = InstrumentStatus.INSTRUMENT_STATUS_BASE;
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(instrumentStatus)
        .build();

      assertIterableEquals(expectedSharesResponse.getInstrumentsList(), service.getSharesSync(instrumentStatus));
      verify(grpcService, times(1)).shares(eq(inArg), any());
      assertIterableEquals(expectedSharesResponse.getInstrumentsList(),  service.getShares(instrumentStatus).join());
      verify(grpcService, times(2)).shares(eq(inArg), any());
    }

    @Test
    void getAll_Test() {
      var inArg = InstrumentsRequest.newBuilder()
        .setInstrumentStatus(InstrumentStatus.INSTRUMENT_STATUS_ALL)
        .build();

      assertIterableEquals(expectedSharesResponse.getInstrumentsList(), service.getAllSharesSync());
      verify(grpcService, times(1)).shares(eq(inArg), any());
      assertIterableEquals(expectedSharesResponse.getInstrumentsList(), service.getAllShares().join());
      verify(grpcService, times(2)).shares(eq(inArg), any());
    }
  }

  @Nested
  class GetAccruedInterestsTest {

    @Test
    void get_Test() {
      var expected = GetAccruedInterestsResponse.newBuilder()
        .addAccruedInterests(
          AccruedInterest.newBuilder().setValuePercent(
            Quotation.newBuilder().setUnits(1).build()).build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getAccruedInterests(GetAccruedInterestsRequest request,
                                          StreamObserver<GetAccruedInterestsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = GetAccruedInterestsRequest.newBuilder()
        .setFigi("figi")
        .setFrom(Timestamp.newBuilder().setSeconds(1234567890).build())
        .setTo(Timestamp.newBuilder().setSeconds(1234567890).setNanos(111222333).build())
        .build();

      assertEquals(expected.getAccruedInterestsList(), service.getAccruedInterestsSync(
        inArg.getFigi(),
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())));
      assertEquals(expected.getAccruedInterestsList(), service.getAccruedInterests(
        inArg.getFigi(),
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())).join());

      verify(grpcService, times(2)).getAccruedInterests(eq(inArg), any());
    }

    @Test
    void get_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getAccruedInterests(GetAccruedInterestsRequest request,
                                          StreamObserver<GetAccruedInterestsResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = GetAccruedInterestsRequest.newBuilder()
        .setFigi("figi")
        .setFrom(Timestamp.newBuilder().setSeconds(1234567890).build())
        .setTo(Timestamp.newBuilder().setSeconds(1234567890).setNanos(111222333).build())
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getAccruedInterestsSync(
        inArg.getFigi(),
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getAccruedInterests(
        inArg.getFigi(),
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())).join());

      verify(grpcService, times(2)).getAccruedInterests(eq(inArg), any());
    }

    @Test
    void get_shouldThrowIfToIsNotAfterFrom_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
        }));
      var service = mkClientBasedOnServer(grpcService);

      var now = Instant.now();
      var nowMinusSecond = now.minusSeconds(1);
      assertThrows(IllegalArgumentException.class, () -> service.getAccruedInterestsSync(
        "figi",
        now,
        nowMinusSecond));
      futureThrown.expect(CompletionException.class);
      futureThrown.expectCause(IsInstanceOf.instanceOf(IllegalArgumentException.class));
      assertThrows(IllegalArgumentException.class, () -> service.getAccruedInterests("figi", now, nowMinusSecond));

      verify(grpcService, never()).getAccruedInterests(any(), any());
    }

  }

  @Nested
  class GetFuturesMarginTest {

    @Test
    void get_Test() {
      var expected = GetFuturesMarginResponse.newBuilder()
        .setInitialMarginOnBuy(MoneyValue.newBuilder().setCurrency("USD").setUnits(1).build())
        .setInitialMarginOnSell(MoneyValue.newBuilder().setCurrency("USD").setUnits(2).build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getFuturesMargin(GetFuturesMarginRequest request,
                                       StreamObserver<GetFuturesMarginResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = GetFuturesMarginRequest.newBuilder()
        .setFigi("figi")
        .build();

      assertEquals(expected, service.getFuturesMarginSync(inArg.getFigi()));
      assertEquals(expected, service.getFuturesMargin(inArg.getFigi()).join());

      verify(grpcService, times(2)).getFuturesMargin(eq(inArg), any());
    }

    @Test
    void get_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getFuturesMargin(GetFuturesMarginRequest request,
                                       StreamObserver<GetFuturesMarginResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = GetFuturesMarginRequest.newBuilder()
        .setFigi("figi")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getFuturesMarginSync(inArg.getFigi()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getFuturesMargin(inArg.getFigi()).join());

      verify(grpcService, times(2)).getFuturesMargin(eq(inArg), any());
    }

  }

  @Nested
  class GetInstrumentTest {

    @Test
    void getByTicker_Test() {
      var expected = InstrumentResponse.newBuilder()
        .setInstrument(Instrument.newBuilder().setTicker("TCS").setClassCode("moex").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getInstrumentBy(InstrumentRequest request,
                                      StreamObserver<InstrumentResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();

      assertEquals(expected.getInstrument(), service.getInstrumentByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertEquals(expected.getInstrument(), service.getInstrumentByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).getInstrumentBy(eq(inArg), any());
    }

    @Test
    void getByTicker_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getInstrumentBy(InstrumentRequest request,
                                      StreamObserver<InstrumentResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_TICKER)
        .setId("TCS")
        .setClassCode("moex")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getInstrumentByTickerSync(inArg.getId(), inArg.getClassCode()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getInstrumentByTicker(inArg.getId(), inArg.getClassCode()).join());

      verify(grpcService, times(2)).getInstrumentBy(eq(inArg), any());
    }

    @Test
    void getByFigi_Test() {
      var expected = InstrumentResponse.newBuilder()
        .setInstrument(Instrument.newBuilder().setFigi("figi").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getInstrumentBy(InstrumentRequest request,
                                      StreamObserver<InstrumentResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertEquals(expected.getInstrument(), service.getInstrumentByFigiSync(inArg.getId()));
      assertEquals(expected.getInstrument(), service.getInstrumentByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).getInstrumentBy(eq(inArg), any());
    }

    @Test
    void getByFigi_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getInstrumentBy(InstrumentRequest request,
                                      StreamObserver<InstrumentResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = InstrumentRequest.newBuilder()
        .setIdType(InstrumentIdType.INSTRUMENT_ID_TYPE_FIGI)
        .setId("figi")
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getInstrumentByFigiSync(inArg.getId()));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getInstrumentByFigi(inArg.getId()).join());

      verify(grpcService, times(2)).getInstrumentBy(eq(inArg), any());
    }

  }

  @Nested
  class GetAssetsTest {

    @Test
    void getAssets_Test() {
      var expected = AssetsResponse.newBuilder()
        .addAssets(Asset.newBuilder().setUid("uid").setType(AssetType.ASSET_TYPE_CURRENCY).build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getAssets(AssetsRequest request, StreamObserver<AssetsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = AssetsRequest.getDefaultInstance();
      var actualSync = service.getAssetsSync();
      var actualAsync = service.getAssets().join();

      assertIterableEquals(expected.getAssetsList(), actualSync);
      assertIterableEquals(expected.getAssetsList(), actualAsync);

      verify(grpcService, times(2)).getAssets(eq(inArg), any());
    }

    @Test
    void getAssetBy_Test() {
      var expected = AssetResponse.newBuilder()
        .setAsset(AssetFull.newBuilder().setUid("uid").setType(AssetType.ASSET_TYPE_CURRENCY).build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getAssetBy(AssetRequest request, StreamObserver<AssetResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = AssetRequest.newBuilder().setId("uid").build();
      var actualSync = service.getAssetBySync("uid");
      var actualAsync = service.getAssetBy("uid").join();

      assertEquals(expected.getAsset(), actualSync);
      assertEquals(expected.getAsset(), actualAsync);

      verify(grpcService, times(2)).getAssetBy(eq(inArg), any());
    }
  }
  @Nested
  class GetDividendsTest {

    @Test
    void get_Test() {
      var expected = GetDividendsResponse.newBuilder()
        .addDividends(Dividend.newBuilder().setDividendType("Regular Cash").build())
        .build();
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getDividends(GetDividendsRequest request,
                                   StreamObserver<GetDividendsResponse> responseObserver) {
            responseObserver.onNext(expected);
            responseObserver.onCompleted();
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = GetDividendsRequest.newBuilder()
        .setFigi("figi")
        .setFrom(Timestamp.newBuilder().setSeconds(1234567890).build())
        .setTo(Timestamp.newBuilder().setSeconds(1234567890).setNanos(111222333).build())
        .build();

      assertEquals(expected.getDividendsList(),  service.getDividendsSync(
        inArg.getFigi(),
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())));
      assertEquals(expected.getDividendsList(), service.getDividends(
        inArg.getFigi(),
        timestampToInstant(inArg.getFrom()),
        timestampToInstant(inArg.getTo())).join());

      verify(grpcService, times(2)).getDividends(eq(inArg), any());
    }

    @Test
    void get_shouldReturnEmptyInCaseOfNotFound_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
          @Override
          public void getDividends(GetDividendsRequest request,
                                   StreamObserver<GetDividendsResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("50002")));
          }
        }));
      var service = mkClientBasedOnServer(grpcService);

      var inArg = GetDividendsRequest.newBuilder()
        .setFigi("figi")
        .setFrom(Timestamp.newBuilder().setSeconds(1234567890).build())
        .setTo(Timestamp.newBuilder().setSeconds(1234567890).setNanos(111222333).build())
        .build();

      assertThrowsApiRuntimeException("50002", () -> service.getDividendsSync(inArg.getFigi(), timestampToInstant(inArg.getFrom()), timestampToInstant(inArg.getTo())));
      assertThrowsAsyncApiRuntimeException("50002", () -> service.getDividends(inArg.getFigi(), timestampToInstant(inArg.getFrom()), timestampToInstant(inArg.getTo())).join());

      verify(grpcService, times(2)).getDividends(eq(inArg), any());
    }

    @Test
    void get_shouldThrowIfToIsNotAfterFrom_Test() {
      var grpcService = mock(InstrumentsServiceGrpc.InstrumentsServiceImplBase.class, delegatesTo(
        new InstrumentsServiceGrpc.InstrumentsServiceImplBase() {
        }));
      var service = mkClientBasedOnServer(grpcService);

      var now = Instant.now();
      var nowMinusSecond = now.minusSeconds(1);
      assertThrows(IllegalArgumentException.class, () -> service.getDividendsSync(
        "figi",
        now,
        nowMinusSecond));
      futureThrown.expect(CompletionException.class);
      futureThrown.expectCause(IsInstanceOf.instanceOf(IllegalArgumentException.class));
      assertThrows(IllegalArgumentException.class, () -> service.getDividends("figi", now, nowMinusSecond));

      verify(grpcService, never()).getDividends(any(), any());
    }
  }
}
