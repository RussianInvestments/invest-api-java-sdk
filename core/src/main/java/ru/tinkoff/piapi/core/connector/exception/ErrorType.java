package ru.tinkoff.piapi.core.connector.exception;

import io.grpc.Status;

import java.util.EnumMap;
import java.util.Map;

public enum ErrorType {
  UNKNOWN,
  UNIMPLEMENTED,
  UNAVAILABLE,
  INVALID_ARGUMENT,
  PERMISSION_DENIED,
  UNAUTHENTICATED,
  NOT_FOUND,
  INTERNAL,
  RESOURCE_EXHAUSTED,
  FAILED_PRECONDITION;

  private static final EnumMap<Status.Code, ErrorType> statusCodeToErrorType = new EnumMap<>(Map.of(
    Status.Code.UNIMPLEMENTED, UNIMPLEMENTED,
    Status.Code.UNAVAILABLE, UNAVAILABLE,
    Status.Code.INVALID_ARGUMENT, INVALID_ARGUMENT,
    Status.Code.PERMISSION_DENIED, PERMISSION_DENIED,
    Status.Code.UNAUTHENTICATED, UNAUTHENTICATED,
    Status.Code.NOT_FOUND, NOT_FOUND,
    Status.Code.INTERNAL, INTERNAL,
    Status.Code.RESOURCE_EXHAUSTED, RESOURCE_EXHAUSTED,
    Status.Code.FAILED_PRECONDITION, FAILED_PRECONDITION,
    Status.Code.UNKNOWN, UNKNOWN
  ));

  public static ErrorType getErrorTypeByStatusCode(Status.Code code) {
    if (!statusCodeToErrorType.containsKey(code)) {
      return UNKNOWN;
    }
    return statusCodeToErrorType.get(code);
  }
}
