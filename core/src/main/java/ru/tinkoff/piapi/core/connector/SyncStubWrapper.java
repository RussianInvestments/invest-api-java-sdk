package ru.tinkoff.piapi.core.connector;

import io.grpc.stub.AbstractBlockingStub;
import ru.tinkoff.piapi.core.connector.exception.ServiceRuntimeException;

import java.util.function.Function;

public class SyncStubWrapper<S extends AbstractBlockingStub<S>> {

  private final S stub;

  public SyncStubWrapper(S stub) {
    this.stub = stub;
  }

  public S getStub() {
    return stub;
  }

  public <T> T callSyncMethod(Function<S, T> call) {
    try {
      return call.apply(stub);
    } catch (Throwable e) {
      throw new ServiceRuntimeException(e);
    }
  }
}
