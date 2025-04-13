package ru.tinkoff.piapi.core.exception;

@Deprecated(since = "1.30", forRemoval = true)
public class ReadonlyModeViolationException extends RuntimeException {
  public ReadonlyModeViolationException() {
    super("Это действие нельзя совершить в режиме \"только для чтения\".");
  }
}
