package ru.tinkoff.piapi.core.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.tinkoff.piapi.core.exception.ReadonlyModeViolationException;
import ru.tinkoff.piapi.core.exception.SandboxModeViolationException;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

  @ParameterizedTest
  @MethodSource("checkPageArguments")
  void checkPage_Test(int input, Class<RuntimeException> exceptionClass) {
    if (input < 0) {
      assertThrows(exceptionClass, () -> ValidationUtils.checkPage(input));
    } else {
      assertDoesNotThrow(() -> ValidationUtils.checkPage(input));
    }
  }

  private static Collection<Arguments> checkPageArguments() {
    return List.of(
      Arguments.of(0, Void.class),
      Arguments.of(Integer.MAX_VALUE, Void.class),
      Arguments.of(-1, IllegalArgumentException.class),
      Arguments.of(Integer.MIN_VALUE, IllegalArgumentException.class)
    );
  }

  @Test
  void checkFromTo_Test() {
    final Instant now = Instant.now();
    assertDoesNotThrow(() -> ValidationUtils.checkFromTo(now, now));
    assertDoesNotThrow(() -> ValidationUtils.checkFromTo(now, now.plusNanos(1)));
    assertThrows(IllegalArgumentException.class, () -> ValidationUtils.checkFromTo(now.plusNanos(1), now));
  }

  @Test
  void checkReadonly_Test() {
    assertDoesNotThrow(() -> ValidationUtils.checkReadonly(false));
    assertThrows(ReadonlyModeViolationException.class, () -> ValidationUtils.checkReadonly(true));
  }

  @Test
  void checkSandbox_Test() {
    assertDoesNotThrow(() -> ValidationUtils.checkSandbox(false));
    assertThrows(SandboxModeViolationException.class, () -> ValidationUtils.checkSandbox(true));
  }

}
