package ru.ttech.piapi.core.connector;

import io.grpc.stub.AbstractBlockingStub;
import ru.ttech.piapi.core.connector.exception.ServiceRuntimeException;

import java.util.function.Function;

/**
 * Обёртка для работы с синхронными gRPC стабами
 */
public class SyncStubWrapper<S extends AbstractBlockingStub<S>> {

  private final S stub;

  SyncStubWrapper(S stub) {
    this.stub = stub;
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
   * <p>Пример вызова:<pre>{@code
   *  var response = syncService.callSyncMethod(stub -> stub.getLastPrices(request));
   * }</pre>
   * @param call Вызов метода gRPC стаба с параметрами
   * @return Результат выполнения запроса
   */
  public <T> T callSyncMethod(Function<S, T> call) {
    try {
      return call.apply(stub);
    } catch (Throwable e) {
      throw new ServiceRuntimeException(e);
    }
  }
}
