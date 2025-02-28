package ru.ttech.piapi.core.impl.orders;

import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.StreamObserver;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamRequest;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamResponse;
import ru.tinkoff.piapi.contract.v1.OrdersStreamServiceGrpc;
import ru.ttech.piapi.core.connector.streaming.ServerSideStreamConfiguration;
import ru.ttech.piapi.core.connector.streaming.listeners.OnCompleteListener;
import ru.ttech.piapi.core.connector.streaming.listeners.OnErrorListener;
import ru.ttech.piapi.core.connector.streaming.listeners.OnNextListener;
import ru.ttech.piapi.core.impl.orders.wrapper.OrderStateWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class OrderStateStreamConfiguration
  extends ServerSideStreamConfiguration<OrdersStreamServiceGrpc.OrdersStreamServiceStub,
  OrderStateStreamRequest, OrderStateStreamResponse> {

  private final List<OnNextListener<OrderStateStreamResponse>> onResponseListeners;

  private OrderStateStreamConfiguration(
    Function<Channel, OrdersStreamServiceGrpc.OrdersStreamServiceStub> stubConstructor,
    MethodDescriptor<OrderStateStreamRequest, OrderStateStreamResponse> method,
    BiConsumer<OrdersStreamServiceGrpc.OrdersStreamServiceStub, StreamObserver<OrderStateStreamResponse>> call,
    List<OnNextListener<OrderStateStreamResponse>> onResponseListeners,
    List<OnNextListener<OrderStateStreamResponse>> onNextListeners,
    List<OnErrorListener> onErrorListeners,
    List<OnCompleteListener> onCompleteListeners
  ) {
    super(stubConstructor, method, call, onNextListeners, onErrorListeners, onCompleteListeners);
    this.onResponseListeners = onResponseListeners;
  }

  public static Builder builder(OrderStateStreamRequest request) {
    return new Builder(
      OrdersStreamServiceGrpc::newStub,
      OrdersStreamServiceGrpc.getOrderStateStreamMethod(),
      (stub, observer) -> stub.orderStateStream(request, observer)
    );
  }

  @Override
  protected Supplier<StreamObserver<OrderStateStreamResponse>> getResponseObserverCreator() {
    return () -> new OrderStateStreamObserver(onResponseListeners, onNextListeners, onErrorListeners, onCompleteListeners);
  }

  public static class Builder
    extends ServerSideStreamConfiguration.Builder<OrdersStreamServiceGrpc.OrdersStreamServiceStub,
    OrderStateStreamRequest, OrderStateStreamResponse> {

    private final List<OnNextListener<OrderStateStreamResponse>> onResponseListeners = new ArrayList<>();

    protected Builder(
      Function<Channel, OrdersStreamServiceGrpc.OrdersStreamServiceStub> stubConstructor,
      MethodDescriptor<OrderStateStreamRequest, OrderStateStreamResponse> method,
      BiConsumer<OrdersStreamServiceGrpc.OrdersStreamServiceStub, StreamObserver<OrderStateStreamResponse>> call
    ) {
      super(stubConstructor, method, call);
    }

    public Builder addOrderStateListener(OnNextListener<OrderStateWrapper> onOrderStateListener) {
      onResponseListeners.add(response -> onOrderStateListener.onNext(new OrderStateWrapper(response.getOrderState())));
      return this;
    }

    public OrderStateStreamConfiguration build() {
      return new OrderStateStreamConfiguration(
        stubConstructor, method, call, onResponseListeners, onNextListeners, onErrorListeners, onCompleteListeners
      );
    }
  }
}
