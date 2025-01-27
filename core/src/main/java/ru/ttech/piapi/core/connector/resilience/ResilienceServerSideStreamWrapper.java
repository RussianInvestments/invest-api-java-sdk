package ru.ttech.piapi.core.connector.resilience;

import io.grpc.stub.AbstractAsyncStub;
import ru.ttech.piapi.core.connector.streaming.ServerSideStreamWrapper;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ResilienceServerSideStreamWrapper<S extends AbstractAsyncStub<S>, RespT> {

  private final ServerSideStreamWrapper<S, RespT> serverSideStreamWrapper;

  public ResilienceServerSideStreamWrapper(
    ServerSideStreamWrapper<S, RespT> serverSideStreamWrapper,
    ScheduledExecutorService executorService,
    long streamTimeout
  ) {
    this.serverSideStreamWrapper = serverSideStreamWrapper;
    serverSideStreamWrapper.getResponseObserver().enableHealthcheck(true);
    executorService.scheduleAtFixedRate(
      () -> checkConnection(serverSideStreamWrapper, streamTimeout),
      0, streamTimeout, TimeUnit.MILLISECONDS
    );
  }

  public void connect() {
    serverSideStreamWrapper.connect();
  }

  public void disconnect() {
    serverSideStreamWrapper.disconnect();
  }

  private void checkConnection(
    ServerSideStreamWrapper<S, RespT> wrapper,
    long timeout
  ) {
    long lastPingTime = wrapper.getResponseObserver().getLastResponseTime();
    long currentTime = System.currentTimeMillis();
    long diff = currentTime - lastPingTime;
    if (wrapper.isConnected() && lastPingTime > 0 && diff > timeout) {
      wrapper.disconnect();
      wrapper.getResponseObserver().resetLastResponseTime();
      wrapper.connect();
    }
  }
}
