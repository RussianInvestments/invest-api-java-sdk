package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class ServerSideStreamConfiguration<S extends AbstractAsyncStub<S>, ReqT, RespT> {
  private final Function<Channel, S> serviceConstructor;
  private final MethodDescriptor<ReqT, RespT> method;
  private final ReqT request;
  private final BiConsumer<S, StreamObserver<RespT>> streamCall;

  private ServerSideStreamConfiguration(
    Function<Channel, S> serviceConstructor,
    MethodDescriptor<ReqT, RespT> method,
    ReqT request,
    BiConsumer<S, StreamObserver<RespT>> streamCall
  ) {
    this.serviceConstructor = serviceConstructor;
    this.method = method;
    this.request = request;
    this.streamCall = streamCall;
  }

  public Function<Channel, S> getServiceConstructor() {
    return serviceConstructor;
  }

  public MethodDescriptor<ReqT, RespT> getMethod() {
    return method;
  }

  public ReqT getRequest() {
    return request;
  }

  public BiConsumer<S, StreamObserver<RespT>> getStreamCall() {
    return streamCall;
  }

  public static <S extends AbstractAsyncStub<S>, ReqT, RespT> Builder<S, ReqT, RespT> builder() {
    return new Builder<>();
  }

  public static class Builder<S extends AbstractAsyncStub<S>, ReqT, RespT> {
    private Function<Channel, S> serviceConstructor;
    private MethodDescriptor<ReqT, RespT> method;
    private ReqT request;
    private BiConsumer<S, StreamObserver<RespT>> streamCall;

    public Builder<S, ReqT, RespT> service(Function<Channel, S> serviceConstructor) {
      this.serviceConstructor = serviceConstructor;
      return this;
    }

    public Builder<S, ReqT, RespT> method(MethodDescriptor<ReqT, RespT> method) {
      this.method = method;
      return this;
    }

    public Builder<S, ReqT, RespT> request(ReqT request) {
      this.request = request;
      return this;
    }

    public ServerSideStreamConfiguration<S, ReqT, RespT> build() {
      return new ServerSideStreamConfiguration<>(serviceConstructor, method, request, streamCall);
    }
  }
}
