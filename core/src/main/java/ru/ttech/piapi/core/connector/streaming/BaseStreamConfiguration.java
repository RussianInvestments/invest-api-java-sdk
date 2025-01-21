package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.AbstractAsyncStub;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public abstract class BaseStreamConfiguration<S extends AbstractAsyncStub<S>, ReqT, RespT> {
  protected final Function<Channel, S> stubConstructor;
  protected final MethodDescriptor<ReqT, RespT> method;
  protected final StreamResponseObserver<RespT> responseObserver;

  protected BaseStreamConfiguration(
    Function<Channel, S> stubConstructor,
    MethodDescriptor<ReqT, RespT> method,
    StreamResponseObserver<RespT> responseObserver
  ) {
    this.stubConstructor = stubConstructor;
    this.method = method;
    this.responseObserver = responseObserver;
  }

  public MethodDescriptor<ReqT, RespT> getMethod() {
    return method;
  }

  public Function<Channel, S> getStubConstructor() {
    return stubConstructor;
  }

  public StreamResponseObserver<RespT> getResponseObserver() {
    return responseObserver;
  }

  protected abstract static class BaseBuilder<
    S extends AbstractAsyncStub<S>,
    ReqT,
    RespT,
    B extends BaseBuilder<S, ReqT, RespT, B>
    > {
    protected final Function<Channel, S> stubConstructor;
    protected final MethodDescriptor<ReqT, RespT> method;
    protected final List<OnNextListener<RespT>> onNextListeners = new LinkedList<>();
    protected final List<OnErrorListener> onErrorListeners = new LinkedList<>();
    protected final List<OnCompleteListener> onCompleteListeners = new LinkedList<>();

    protected BaseBuilder(
      Function<Channel, S> stubConstructor,
      MethodDescriptor<ReqT, RespT> method
    ) {
      this.stubConstructor = stubConstructor;
      this.method = method;
    }

    @SuppressWarnings("unchecked")
    public B addOnNextListener(OnNextListener<RespT> onNextListener) {
      onNextListeners.add(onNextListener);
      return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B addOnErrorListener(OnErrorListener onErrorListener) {
      onErrorListeners.add(onErrorListener);
      return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B addOnCompleteListener(OnCompleteListener onCompleteListener) {
      onCompleteListeners.add(onCompleteListener);
      return (B) this;
    }

    protected StreamResponseObserver<RespT> createResponseObserver() {
      return new StreamResponseObserver<>(onNextListeners, onErrorListeners, onCompleteListeners);
    }
  }
}
