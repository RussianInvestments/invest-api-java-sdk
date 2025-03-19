package ru.ttech.piapi.example.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamRequest;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.impl.orders.OrderStateStreamWrapperConfiguration;

import java.util.concurrent.Executors;

public class OrderStateStreamExample {

  private static final Logger logger = LoggerFactory.getLogger(OrderStateStreamExample.class);

  public static void main(String[] args) {
    var configuration = ConnectorConfiguration.loadPropertiesFromResources("invest.properties");
    var factory = ServiceStubFactory.create(configuration);
    var streamFactory = StreamServiceStubFactory.create(factory);
    var executorService = Executors.newSingleThreadScheduledExecutor();
    var wrapper = streamFactory.newResilienceServerSideStream(OrderStateStreamWrapperConfiguration.builder(executorService)
      .addOnResponseListener(orderState -> logger.info("Order state: {}", orderState))
      .addOnConnectListener(() -> logger.info("Successful reconnection!"))
      .build());
    var request = OrderStateStreamRequest.newBuilder()
      .addAccounts("2092593581")
      .build();
    wrapper.subscribe(request);
    try {
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
