package ru.ttech.piapi.core.helpers;

import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Маппер для конвертации объектов времени и даты
 */
public class TimeMapper {

  private static final ZoneOffset DEFAULT_ZONE_OFFSET = ZoneOffset.UTC;

  /**
   * Преобразование {@link Timestamp} в {@link LocalDate}.
   *
   * @param timestamp Экземпляр google {@link Timestamp}.
   * @return Эквивалентный {@link LocalDate}.
   */
  public static LocalDate timestampToLocalDate(Timestamp timestamp) {
    return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos())
      .atZone(DEFAULT_ZONE_OFFSET)
      .toLocalDate();
  }
}
