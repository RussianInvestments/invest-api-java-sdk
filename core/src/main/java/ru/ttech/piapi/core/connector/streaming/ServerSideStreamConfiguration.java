package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class ServerSideStreamConfiguration<S extends AbstractAsyncStub<S>, ReqT, RespT>
  extends BaseStreamConfiguration<S, ReqT, RespT> {

  private final BiConsumer<S, StreamObserver<RespT>> call;

  private ServerSideStreamConfiguration(
    Function<Channel, S> stubConstructor,
    MethodDescriptor<ReqT, RespT> method,
    BiConsumer<S, StreamObserver<RespT>> call,
    StreamResponseObserver<RespT> responseObserver
  ) {
    super(stubConstructor, method, responseObserver);
    this.call = call;
  }

  public BiConsumer<S, StreamObserver<RespT>> getCall() {
    return call;
  }

  public static <S extends AbstractAsyncStub<S>, ReqT, RespT> Builder<S, ReqT, RespT> builder(
    Function<Channel, S> stubConstructor,
    MethodDescriptor<ReqT, RespT> method,
    BiConsumer<S, StreamObserver<RespT>> call
  ) {
    return new Builder<>(stubConstructor, method, call);
  }

  public static class Builder<S extends AbstractAsyncStub<S>, ReqT, RespT>
    extends BaseBuilder<S, ReqT, RespT, Builder<S, ReqT, RespT>> {

    private final BiConsumer<S, StreamObserver<RespT>> call;

    private Builder(
      Function<Channel, S> stubConstructor,
      MethodDescriptor<ReqT, RespT> method,
      BiConsumer<S, StreamObserver<RespT>> call
    ) {
      super(stubConstructor, method);
      this.call = call;
    }

    public ServerSideStreamConfiguration<S, ReqT, RespT> build() {
      return new ServerSideStreamConfiguration<>(stubConstructor, method, call, createResponseObserver());
    }
  }
}
