package ru.ttech.piapi.example.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.OrderStateStreamRequest;
import ru.ttech.piapi.core.connector.ConnectorConfiguration;
import ru.ttech.piapi.core.connector.ServiceStubFactory;
import ru.ttech.piapi.core.connector.streaming.StreamServiceStubFactory;
import ru.ttech.piapi.core.impl.orders.OrderStateStreamWrapper;

import java.util.concurrent.Executors;

public class OrderStateStreamExample {

  private static final Logger logger = LoggerFactory.getLogger(OrderStateStreamExample.class);

  public static void main(String[] args) {
    var configuration = ConnectorConfiguration.loadFromPropertiesFile("invest.properties");
    var factory = ServiceStubFactory.create(configuration);
    var streamFactory = StreamServiceStubFactory.create(factory);
    var executorService = Executors.newSingleThreadScheduledExecutor();
    var wrapper = new OrderStateStreamWrapper(
      streamFactory,
      executorService,
      orderState -> logger.info("Order state: {}", orderState),
      () -> logger.info("Successful reconnection!")
    );
    var request = OrderStateStreamRequest.newBuilder()
      .addAccounts("2092593581")
      .setPingDelayMillis(1000)
      .build();
    wrapper.subscribe(request);
    try {
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
