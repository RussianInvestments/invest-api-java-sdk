package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;
import ru.ttech.piapi.core.connector.ServiceStubFactory;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class StreamServiceStubFactory<S extends AbstractAsyncStub<S>> {
  private final S stub;

  private StreamServiceStubFactory(ServiceStubFactory serviceStubFactory, Function<Channel, S> constructor) {
    this.stub = constructor.apply(serviceStubFactory.getChannel());
  }

  public static <S extends AbstractAsyncStub<S>> StreamServiceStubFactory<S> create(
    ServiceStubFactory serviceStubFactory,
    Function<Channel, S> constructor
  ) {
    return new StreamServiceStubFactory<>(serviceStubFactory, constructor);
  }

  public <RespT> ServerSideStreamWrapper<S, RespT> newServerSideStream(
    MethodDescriptor<?, RespT> method,
    BiConsumer<S, StreamObserver<RespT>> call
  ) {
    return new ServerSideStreamWrapper<>(stub, call);
  }

  public <ReqT, RespT> ServerSideStreamWrapper<S, RespT> newServerSideStream(
    ServerSideStreamConfiguration<S, ReqT, RespT> configuration
  ) {
    return newServerSideStream(
      configuration.getMethod(),
      configuration.getStreamCall()
    );
  }

  public <ReqT, RespT> BidirectionalStreamWrapper<S, ReqT, RespT> newBidirectionalStream(
    MethodDescriptor<ReqT, RespT> method,
    BiFunction<S, StreamObserver<RespT>, StreamObserver<ReqT>> call
  ) {
    return new BidirectionalStreamWrapper<>(stub, call);
  }
}
