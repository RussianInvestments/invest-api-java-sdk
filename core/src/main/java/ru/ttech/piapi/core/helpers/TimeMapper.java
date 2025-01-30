package ru.ttech.piapi.core.helpers;

import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Function;

/**
 * Маппер для конвертации объектов времени и даты
 */
public class TimeMapper {

  private static final ZoneOffset DEFAULT_ZONE_OFFSET = ZoneOffset.UTC;
  private static final Function<Timestamp, LocalDateTime> convert = val -> mapSecondsAndNanos(val.getSeconds(), val.getNanos());

  /**
   * Преобразование {@link Timestamp} в {@link LocalDate}.
   *
   * @param timestamp Экземпляр google {@link Timestamp}.
   * @return Эквивалентный {@link LocalDate}.
   */
  public static LocalDateTime timestampToLocalDateTime(Timestamp timestamp) {
    return Optional.ofNullable(timestamp)
      .map(convert)
      .orElseGet(() -> convert.apply(Timestamp.getDefaultInstance()));
  }

  private static LocalDateTime mapSecondsAndNanos(long seconds, long nanos) {
    return Instant.ofEpochSecond(seconds, nanos).atZone(DEFAULT_ZONE_OFFSET).toLocalDateTime();
  }
}
