package ru.ttech.piapi.core.connector.streaming;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public class ServerSideStreamWrapper<S, RespT> {

  private static final Logger logger = LoggerFactory.getLogger(ServerSideStreamWrapper.class);
  private final S stub;
  private final AtomicReference<Context.CancellableContext> contextRef = new AtomicReference<>();

  private final BiConsumer<S, StreamObserver<RespT>> call;
  private final StreamObserver<RespT> responseObserver;

  public ServerSideStreamWrapper(S stub, BiConsumer<S, StreamObserver<RespT>> call) {
    this.responseObserver = newResponseObserver();
    this.stub = stub;
    this.call = call;
  }

  public void subscribe() {
    var context = Context.current().fork().withCancellation();
    var ctx = context.attach();
    try {
      // подписываемся на стрим
      call.accept(stub, responseObserver);
      contextRef.set(context);
    } finally {
      context.detach(ctx);
    }
  }

  private StreamObserver<RespT> newResponseObserver() {
    return new StreamObserver<>() {
      @Override
      public void onNext(RespT t) {
        logger.info("onNext: {}", t);
      }

      @Override
      public void onError(Throwable throwable) {
        logger.info("onError: {}", throwable.toString());
      }

      @Override
      public void onCompleted() {
        logger.info("onCompleted");
      }
    };
  }
}
