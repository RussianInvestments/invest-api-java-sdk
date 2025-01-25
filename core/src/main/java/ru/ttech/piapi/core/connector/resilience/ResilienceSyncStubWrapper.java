package ru.ttech.piapi.core.connector.resilience;

import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractBlockingStub;
import ru.ttech.piapi.core.connector.SyncStubWrapper;

import java.util.function.Function;

public class ResilienceSyncStubWrapper<S extends AbstractBlockingStub<S>> {

  private final SyncStubWrapper<S> syncStubWrapper;
  private final ResilienceConfiguration resilienceConfiguration;

  public ResilienceSyncStubWrapper(
    SyncStubWrapper<S> syncStubWrapper,
    ResilienceConfiguration resilienceConfiguration
  ) {
    this.syncStubWrapper = syncStubWrapper;
    this.resilienceConfiguration = resilienceConfiguration;
  }

  public <T> T callSyncMethod(MethodDescriptor<?, T> method, Function<S, T> call) {
    return resilienceConfiguration.getRetryForMethod(method)
      .executeSupplier(() -> syncStubWrapper.callSyncMethod(call));
  }

  public SyncStubWrapper<S> getSyncStubWrapper() {
    return syncStubWrapper;
  }
}
