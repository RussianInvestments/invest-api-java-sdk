package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;

import java.util.function.BiFunction;
import java.util.function.Function;

public class BidirectionalStreamConfiguration<S extends AbstractAsyncStub<S>, ReqT, RespT>
  extends BaseStreamConfiguration<S, ReqT, RespT> {

  private final BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> call;

  private BidirectionalStreamConfiguration(
    Function<Channel, S> stubConstructor,
    MethodDescriptor<ReqT, RespT> method,
    BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> call,
    StreamResponseObserver<RespT> responseObserver
  ) {
    super(stubConstructor, method, responseObserver);
    this.call = call;
  }

  public BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> getCall() {
    return call;
  }

  public static <S extends AbstractAsyncStub<S>, ReqT, RespT> Builder<S, ReqT, RespT> builder(
    Function<Channel, S> stubConstructor,
    MethodDescriptor<ReqT, RespT> method,
    BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> call
  ) {
    return new Builder<>(stubConstructor, method, call);
  }

  public static class Builder<S extends AbstractAsyncStub<S>, ReqT, RespT>
    extends BaseBuilder<S, ReqT, RespT, Builder<S, ReqT, RespT>> {

    private final BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> call;

    private Builder(
      Function<Channel, S> stubConstructor,
      MethodDescriptor<ReqT, RespT> method,
      BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> call
    ) {
      super(stubConstructor, method);
      this.call = call;
    }

    public BidirectionalStreamConfiguration<S, ReqT, RespT> build() {
      return new BidirectionalStreamConfiguration<>(stubConstructor, method, call, createResponseObserver());
    }
  }
}
