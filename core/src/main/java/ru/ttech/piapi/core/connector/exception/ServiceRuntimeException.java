package ru.ttech.piapi.core.connector.exception;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class ServiceRuntimeException extends RuntimeException {

  private static final Metadata.Key<String> descriptionKey = Metadata.Key.of("message", Metadata.ASCII_STRING_MARSHALLER);

  private static final String DEFAULT_ERROR_DESCRIPTION = "Unknown error";

  private final Throwable throwable;
  private final Status.Code errorType;
  private final String errorCode;
  private final String description;
  private final String trackingId;
  private final Metadata metadata;

  public ServiceRuntimeException(Throwable exception) {
    super();
    this.throwable = exception;
    this.metadata = getMetadata(exception);
    this.description = getErrorDescription(this.metadata);
    this.trackingId = getHeader("x-tracking-id", this.metadata);
    Status errorStatus = Status.fromThrowable(exception);
    this.errorType = errorStatus.getCode();
    this.errorCode = getErrorCode(errorStatus);
  }

  private String getErrorDescription(Metadata metadata) {
    if (metadata != null && metadata.containsKey(descriptionKey)) {
      return metadata.get(descriptionKey);
    }
    return DEFAULT_ERROR_DESCRIPTION;
  }

  private String getHeader(String headerName, Metadata metadata) {
    if (metadata != null) {
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

  private String getErrorCode(Status status) {
    return status.getDescription();
  }

  public Throwable getThrowable() {
    return throwable;
  }

  public String getErrorCode() {
    return errorCode;
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

  public Status.Code getErrorType() {
    return errorType;
  }

  @Override
  public String toString() {
    return "ServiceRuntimeException{" +
      "throwable=" + throwable +
      ", errorType=" + errorType +
      ", errorCode='" + errorCode + '\'' +
      ", description='" + description + '\'' +
      ", trackingId='" + trackingId + '\'' +
      ", metadata=" + metadata +
      '}';
  }
}
