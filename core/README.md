# Модуль java-sdk-core
## Унарные запросы
```mermaid
classDiagram
direction RL
class ResilienceConfigurationBuilder {
  + withCircuitBreakerForMethod(MethodDescriptor~?, ?~, CircuitBreakerConfig)
  + withDefaultCircuitBreaker(CircuitBreakerConfig)
  + withRateLimiterForMethod(MethodDescriptor~?, ?~, RateLimiterConfig)
  + withDefaultRateLimiter(RateLimiterConfig)
  + withDefaultRetry(RetryConfig)
  + withRetryForMethod(MethodDescriptor~?, ?~, RetryConfig)
  + withBulkheadForMethod(MethodDescriptor~?, ?~, BulkheadConfig)
  + withDefaultBulkHead(BulkheadConfig)
  + build()
}
class AsyncStubWrapper~S~ {
    + callAsyncMethod(BiConsumer~S, StreamObserver~T~~) CompletableFuture~T~
}
class ResilienceAsyncStubWrapper~S~ {
  + callAsyncMethod(MethodDescriptor~?, T~, BiConsumer~S, StreamObserver~T~~)
}
class ResilienceConfiguration {
  + builder(ScheduledExecutorService, ConnectorConfiguration)
}
class ResilienceSyncStubWrapper~S~ {
  + callSyncMethod(MethodDescriptor~?, T~, Function~S, T~)
}
class ServiceStubFactory {
  + create(ConnectorConfiguration)
  + newAsyncService(Function~Channel, S~)
  + newSyncService(Function~Channel, S~)
  + newResilienceSyncService(Function~Channel, S~, ResilienceConfiguration)
  + newResilienceAsyncService(Function~Channel, S~, ResilienceConfiguration)
}
class SyncStubWrapper~S~ {
  + callSyncMethod(Function~S, T~)
}
ResilienceConfiguration  -->  ResilienceConfigurationBuilder
ResilienceConfigurationBuilder  ..>  ResilienceConfiguration : «create»
ResilienceAsyncStubWrapper~S~ "1" *--> "asyncStubWrapper 1" AsyncStubWrapper~S~
ResilienceAsyncStubWrapper~S~ "1" *--> "resilienceConfiguration 1" ResilienceConfiguration
ResilienceConfiguration  ..>  ResilienceConfigurationBuilder : «create»
ResilienceSyncStubWrapper~S~ "1" *--> "resilienceConfiguration 1" ResilienceConfiguration
ResilienceSyncStubWrapper~S~ "1" *--> "syncStubWrapper 1" SyncStubWrapper~S~
ServiceStubFactory  ..>  AsyncStubWrapper~S~ : «create»
ServiceStubFactory  ..>  ResilienceAsyncStubWrapper~S~ : «create»
ServiceStubFactory  ..>  ResilienceSyncStubWrapper~S~ : «create»
ServiceStubFactory  ..>  SyncStubWrapper~S~ : «create»
```
Есть два подхода при работе с унарными запросами:
 * **Синхронные запросы**
 <br> Такие запросы блокируют выполнение кода, пока не придёт ответ от сервера.
 Чтобы выполнить такой запрос, необходимо будет создать экземпляр `SyncStubWrapper`:
   ```java
   class Main {
    public static void main(String[] args){
         var configuration = ConnectorConfiguration.loadFromPropertiesFile("invest.properties");
         var unaryServiceFactory = ServiceStubFactory.create(configuration);
         var instrumentsService = unaryServiceFactory.newSyncService(InstrumentsServiceGrpc::newBlockingStub);
         var response = instrumentsService.callSyncMethod(stub -> stub.shares(InstrumentsRequest.getDefaultInstance()));
    }
   }
   ```
   Также можно создать resilience-версию `ResilienceSyncStubWrapper`, которая поддерживает retry, bulkhead, rate-limiting и circuit-breaker:
   ```java
   class Main {
    public static void main(String[] args){
        var configuration = ConnectorConfiguration.loadFromPropertiesFile("invest.properties");
        var unaryServiceFactory = ServiceStubFactory.create(configuration);
        var executorService = Executors.newSingleThreadScheduledExecutor();
        var instrumentsResilienceService = unaryServiceFactory.newResilienceSyncService(
            InstrumentsServiceGrpc::newBlockingStub,
            ResilienceConfiguration.builder(executorService, configuration)
                .withDefaultRetry(RetryConfig.custom().waitDuration(Duration.ofMillis(3000)).maxAttempts(5).build())
                .build()
        );
        var response = instrumentsResilienceService.callSyncMethod(
            InstrumentsServiceGrpc.getSharesMethod(),
            stub -> stub.shares(InstrumentsRequest.getDefaultInstance())
        );
    }
   }
   ```
   * **Асинхронные запросы**
   <br>Запросы, возвращающие `CompletableFuture`, который впоследствии может быть обработан так, как Вам необходимо.
   Чтобы выполнить такой запрос, необходимо будет создать экземпляр `AsyncStubWrapper`:
     ```java
     class Main {
        public static void main(String[] args) {
            var configuration = ConnectorConfiguration.loadFromPropertiesFile("invest.properties");
            var unaryServiceFactory = ServiceStubFactory.create(configuration);
            var instrumentsService = unaryServiceFactory.newAsyncService(MarketDataServiceGrpc::newStub);
            var request = GetLastPricesRequest.newBuilder()
                .addInstrumentId("87db07bc-0e02-4e29-90bb-05e8ef791d7b")
                .build();
            CompletableFuture<GetLastPricesResponse> response = instrumentsService.callAsyncMethod(
                (stub, observer) -> stub.getLastPrices(request, observer)
            );
        }
     }
     ```
     Также можно создать resilience-версию `ResilienceAsyncStubWrapper`, которая поддерживает retry,
     bulkhead, rate-limiting и circuit-breaker:
     ```java
     class Main {
        public static void main(String[] args) {
           var configuration = ConnectorConfiguration.loadFromPropertiesFile("invest.properties");
           var unaryServiceFactory = ServiceStubFactory.create(configuration);
           var executorService = Executors.newSingleThreadScheduledExecutor();
           var instrumentsService = unaryServiceFactory.newResilienceAsyncService(
                MarketDataServiceGrpc::newStub,
                ResilienceConfiguration.builder(executorService, configuration)
                    .withDefaultRetry(RetryConfig.custom().waitDuration(Duration.ofMillis(3000)).maxAttempts(5).build())
                    .build()
           );
           var request = GetLastPricesRequest.newBuilder()
                   .addInstrumentId("87db07bc-0e02-4e29-90bb-05e8ef791d7b")
                   .build();
           CompletableFuture<GetLastPricesResponse> response = instrumentsService.callAsyncMethod(
                MarketDataServiceGrpc.getGetLastPricesMethod(),
                (stub, observer) -> stub.getLastPrices(request, observer)
           );
        }
     }
     ```
 _Примечание: конфигурация по умолчанию настроена только для retry,
 но Вы можете самостоятельно настроить нужную конфигурацию для остальных компонент resilience_

## Server-side и bidirectional стримы
```mermaid
classDiagram
direction RL
class BidirectionalStreamConfiguration~S, ReqT, RespT~ {
  + builder(Function~Channel, S~, MethodDescriptor~ReqT, RespT~, BiFunction~S, StreamObserver~RespT~, StreamObserver~ReqT~~)
}
class ServerSideStreamConfiguration~S, ReqT, RespT~ {
  + builder(Function~Channel, S~, MethodDescriptor~ReqT, RespT~, BiConsumer~S, StreamObserver~RespT~~)
}
class BidirectionalStreamWrapper~S, ReqT, RespT~ {
  + disconnect() void
  + connect() void
  + newCall(ReqT) void
}
class ServerSideStreamConfigurationBuilder~S, ReqT, RespT~ {
  + addOnNextListener(OnNextListener onNextListener)
  + addOnErrorListener(OnErrorListener onErrorListener)
  + addOnCompleteListener(OnCompleteListener onCompleteListener)
  + build()
}
class BidirectionalStreamConfigurationBuilder~S, ReqT, RespT~ {
  + addOnNextListener(OnNextListener onNextListener)
  + addOnErrorListener(OnErrorListener onErrorListener)
  + addOnCompleteListener(OnCompleteListener onCompleteListener)
  + build()
}
class ServerSideStreamWrapper~S, RespT~ {
  + disconnect() void
  + connect() void
}
class StreamServiceStubFactory {
  + newServerSideStream(ServerSideStreamConfiguration~S, ReqT, RespT~)
  + newBidirectionalStream(BidirectionalStreamConfiguration~S, ReqT, RespT~)
  + create(ServiceStubFactory)
}

BidirectionalStreamConfiguration~S, ReqT, RespT~  ..>  BidirectionalStreamConfigurationBuilder~S, ReqT, RespT~ : «create»
BidirectionalStreamConfiguration~S, ReqT, RespT~  -->  BidirectionalStreamConfigurationBuilder~S, ReqT, RespT~
BidirectionalStreamConfigurationBuilder~S, ReqT, RespT~  ..>  BidirectionalStreamConfiguration~S, ReqT, RespT~ : «create»
ServerSideStreamConfiguration~S, ReqT, RespT~  -->  ServerSideStreamConfigurationBuilder~S, ReqT, RespT~
ServerSideStreamConfigurationBuilder~S, ReqT, RespT~  ..>  ServerSideStreamConfiguration~S, ReqT, RespT~ : «create»
ServerSideStreamConfiguration~S, ReqT, RespT~  ..>  ServerSideStreamConfigurationBuilder~S, ReqT, RespT~ : «create»
StreamServiceStubFactory  ..>  BidirectionalStreamWrapper~S, ReqT, RespT~ : «create»
StreamServiceStubFactory  ..>  ServerSideStreamWrapper~S, RespT~ : «create»
```
