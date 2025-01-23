package ru.ttech.piapi.core.connector;

import io.github.resilience4j.retry.RetryRegistry;
import io.grpc.stub.AbstractBlockingStub;
import ru.ttech.piapi.core.connector.exception.ServiceRuntimeException;

import java.util.function.Function;

/**
 * Обёртка для работы с синхронными gRPC стабами
 */
public class SyncStubWrapper<S extends AbstractBlockingStub<S>> {

  private final S stub;
  private final RetryRegistry retryRegistry;

  SyncStubWrapper(S stub, RetryRegistry retryRegistry) {
    this.stub = stub;
    this.retryRegistry = retryRegistry;
  }

  /**
   * Метод для прямого получения gRPC стаба
   *
   * @return gRPC стаб
   */
  public S getStub() {
    return stub;
  }

  /**
   * Метод для синхронного вызова метода стаба
   *
   * @param call Вызов метода gRPC стаба с параметрами
   * @return Результат выполнения запроса
   */
  public <T> T callSyncMethod(Function<S, T> call) {
    return retryRegistry.retry("invest").executeSupplier(() -> {
      try {
        return call.apply(stub);
      } catch (Throwable e) {
        throw new ServiceRuntimeException(e);
      }
    });
  }
}
