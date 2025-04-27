package ru.tinkoff.piapi.core.exception;

@Deprecated(since = "1.30", forRemoval = true)
public class SandboxModeViolationException extends RuntimeException {
  public SandboxModeViolationException() {
    super("Это действие нельзя совершить в режиме \"песочницы\".");
  }
}
