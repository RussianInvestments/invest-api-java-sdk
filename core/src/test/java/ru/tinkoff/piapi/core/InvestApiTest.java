package ru.tinkoff.piapi.core;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import ru.tinkoff.piapi.contract.v1.InstrumentsServiceGrpc;
import ru.tinkoff.piapi.contract.v1.TradingSchedulesRequest;
import ru.tinkoff.piapi.contract.v1.TradingSchedulesResponse;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

public class InvestApiTest {

  private static final String TEST_CONFIG_RESOURCE_NAME = "config.properties";

  @TestFactory
  Collection<DynamicTest> allDefaultPropertiesAreLoadedFromConfigFile() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Properties testProps = getCurrentProperties();
    var method = InvestApi.class.getDeclaredMethod("loadProps");
    method.setAccessible(true);
    Properties props = (Properties) method.invoke(InvestApi.class);

    List<DynamicTest> tests = new ArrayList<>(props.size());

    String testTitle = "Property '%s' is loaded";
    testProps.forEach((key, value) -> {
      tests.add(DynamicTest.dynamicTest(String.format(testTitle, key.toString()), () -> assertEquals(value, props.getProperty(key.toString()))));
    });

    return tests;
  }

  private Properties getCurrentProperties() {
    var loader = Thread.currentThread().getContextClassLoader();
    var props = new Properties();
    try (var resourceStream = loader.getResourceAsStream(TEST_CONFIG_RESOURCE_NAME)) {
      props.load(resourceStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return props;
  }

  @Test
  void creationAlwaysUsesPassedChannel() {
    var channel = InvestApi.defaultChannel("token", null);

    var api = InvestApi.create(channel);
    assertSame(channel, api.getChannel(), "Simple creation doesn't use passed Channel.");

    var readonlyApi = InvestApi.createReadonly(channel);
    assertSame(channel, readonlyApi.getChannel(), "Readonly creation doesn't use passed Channel.");

    var sandboxApi = InvestApi.createSandbox(channel);
    assertSame(channel, sandboxApi.getChannel(), "Sandbox creation doesn't use passed Channel.");
  }

  @Test
  void simpleCreationProducesNotReadonlyNorSandbox() {
    var channel = InvestApi.defaultChannel("token", null);

    var api = InvestApi.create(channel);
    assertFalse(api.isReadonlyMode(), "Simple creation produces readonly mode.");
  }

  @Test
  void readonlyCreationProducesReadonlyOnly() {
    var channel = InvestApi.defaultChannel("token", null);

    var readonlyApi = InvestApi.createReadonly(channel);
    assertTrue(readonlyApi.isReadonlyMode(), "Readonly creation doesn't produce readonly mode.");
  }

  @Test
  void sandboxCreationProducesSandboxOnly() {
    var channel = InvestApi.defaultChannel("token", null);

    var sandboxApi = InvestApi.createSandbox(channel);
    assertFalse(sandboxApi.isReadonlyMode(), "Sandbox creation produces readonly mode.");
  }

  @Test
  void instrumentsServiceIsAlwaysAllowed() {
    var channel = InvestApi.defaultChannel("token", null);

    var api = InvestApi.create(channel);
    assertDoesNotThrow(api::getInstrumentsService);
    var readonlyApi = InvestApi.createReadonly(channel);
    assertDoesNotThrow(readonlyApi::getInstrumentsService);
    var sandboxApi = InvestApi.createReadonly(channel);
    assertDoesNotThrow(sandboxApi::getInstrumentsService);
  }

  @Test
  void marketDataServiceIsAlwaysAllowed() {
    var channel = InvestApi.defaultChannel("token", null);

    var api = InvestApi.create(channel);
    assertDoesNotThrow(api::getMarketDataService);
    var readonlyApi = InvestApi.createReadonly(channel);
    assertDoesNotThrow(readonlyApi::getMarketDataService);
    var sandboxApi = InvestApi.createReadonly(channel);
    assertDoesNotThrow(sandboxApi::getMarketDataService);
  }
}
