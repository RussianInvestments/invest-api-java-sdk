package ru.ttech.piapi.core.connector.exception;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.Optional;

public class ServiceRuntimeException extends RuntimeException {

  private static final String MESSAGE_KEY = "message";
  private static final String TRACKING_ID_KEY = "x-tracking-id";
  private static final String RATELIMIT_RESET_KEY = "x-ratelimit-reset";
  private static final String DEFAULT_ERROR_DESCRIPTION = "Unknown error";
  private final Throwable throwable;
  private final Status errorStatus;
  private final String description;
  private final String trackingId;
  private final Metadata metadata;

  public ServiceRuntimeException(Throwable exception) {
    super(exception);
    this.throwable = exception;
    this.metadata = getMetadata(exception);
    this.description = getHeader(MESSAGE_KEY, this.metadata, DEFAULT_ERROR_DESCRIPTION);
    this.trackingId = getHeader(TRACKING_ID_KEY, this.metadata);
    this.errorStatus = Status.fromThrowable(exception);
  }

  private String getHeader(String headerName, Metadata metadata, String defaultValue) {
    return Optional.ofNullable(getHeader(headerName, metadata))
      .orElse(defaultValue);
  }

  private String getHeader(String headerName, Metadata metadata) {
    if (metadata != null && metadata.containsKey(Metadata.Key.of(headerName, Metadata.ASCII_STRING_MARSHALLER))) {
      return metadata.get(Metadata.Key.of(headerName, Metadata.ASCII_STRING_MARSHALLER));
    }
    return null;
  }

  private Metadata getMetadata(Throwable exception) {
    if (!(exception instanceof StatusRuntimeException)) {
      return null;
    }
    return ((StatusRuntimeException) exception).getTrailers();
  }

  public Throwable getThrowable() {
    return throwable;
  }

  public Status getErrorStatus() {
    return errorStatus;
  }

  public String getErrorCode() {
    return errorStatus.getDescription();
  }

  public String getDescription() {
    return description;
  }

  public String getTrackingId() {
    return trackingId;
  }

  public Metadata getMetadata() {
    return metadata;
  }

  public int getRateLimitReset() {
    try {
      return Optional.ofNullable(getHeader(RATELIMIT_RESET_KEY, metadata))
        .map(Integer::parseInt)
        .orElse(0);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public int parseErrorCode() {
    try {
      return Optional.ofNullable(getErrorCode())
        .map(Integer::parseInt)
        .orElse(0);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public Status.Code getErrorType() {
    return errorStatus.getCode();
  }

  @Override
  public String toString() {
    return "ServiceRuntimeException{" +
      "throwable=" + throwable +
      ", errorType=" + errorStatus.getCode() +
      ", errorCode='" + errorStatus.getDescription() + '\'' +
      ", description='" + description + '\'' +
      ", trackingId='" + trackingId + '\'' +
      ", metadata=" + metadata +
      '}';
  }
}
