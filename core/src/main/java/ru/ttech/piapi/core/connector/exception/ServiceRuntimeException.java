package ru.ttech.piapi.core.connector.exception;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class ServiceRuntimeException extends RuntimeException {

  private static final String MESSAGE_KEY = "message";
  private static final String TRACKING_ID_KEY = "x-tracking-id";
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
    String value = getHeader(headerName, metadata);
    return value == null ? defaultValue : value;
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
    String rateLimitReset = getHeader("x-ratelimit-reset", metadata);
    return rateLimitReset != null
      ? Integer.parseInt(rateLimitReset)
      : 0;
  }

  public int parseErrorCode() {
    try {
      if (errorStatus.getDescription() == null) {
        return 0;
      }
      return Integer.parseInt(errorStatus.getDescription());
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
