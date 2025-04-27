package ru.tinkoff.piapi.core.exception;

import io.grpc.Metadata;
import lombok.Getter;

@Getter
@Deprecated(since = "1.30", forRemoval = true)
public class ApiRuntimeException extends RuntimeException {

  private final Throwable throwable;
  private final String code;
  private final String message;
  private final String trackingId;
  private final Metadata metadata;

  public ApiRuntimeException(String message, String code, String trackingId, Throwable throwable, Metadata metadata) {
    super(String.format("%s : %s tracking_id %s", code, message, trackingId), throwable);
    this.metadata = metadata;
    this.throwable = throwable;
    this.message = String.format("%s : %s Tracking_id : %s", code, message, trackingId);
    this.code = code;
    this.trackingId = trackingId;
  }
}
