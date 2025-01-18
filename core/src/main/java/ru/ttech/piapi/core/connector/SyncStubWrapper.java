package ru.ttech.piapi.core.connector;

import io.grpc.Context;
import io.grpc.stub.AbstractBlockingStub;
import ru.ttech.piapi.core.connector.exception.ServiceRuntimeException;

import java.util.function.Function;

/**
 * Обёртка для работы с синхронными gRPC стабами
 */
public class SyncStubWrapper<S extends AbstractBlockingStub<S>> {

  private final boolean contextFork;
  private final S stub;

  SyncStubWrapper(S stub, boolean contextFork) {
    this.stub = stub;
    this.contextFork = contextFork;
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
    if (!contextFork) {
      try {
        return call.apply(stub);
      } catch (Throwable e) {
        throw new ServiceRuntimeException(e);
      }
    }
    Context newContext = Context.current().fork();
    Context origContext = newContext.attach();
    try {
      return call.apply(stub);
    } catch (Throwable e) {
      throw new ServiceRuntimeException(e);
    } finally {
      newContext.detach(origContext);
    }
  }
}
