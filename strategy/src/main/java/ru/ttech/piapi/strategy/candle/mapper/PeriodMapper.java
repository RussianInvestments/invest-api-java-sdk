package ru.ttech.piapi.strategy.candle.mapper;

import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;

import java.time.Duration;
import java.time.LocalDateTime;

public class PeriodMapper {

  public static Duration getTimePeriod(LocalDateTime periodStart, SubscriptionInterval interval) {
    switch (interval) {
      case SUBSCRIPTION_INTERVAL_ONE_MINUTE:
        return Duration.ofMinutes(1);
      case SUBSCRIPTION_INTERVAL_2_MIN:
        return Duration.ofMinutes(2);
      case SUBSCRIPTION_INTERVAL_3_MIN:
        return Duration.ofMinutes(3);
      case SUBSCRIPTION_INTERVAL_FIVE_MINUTES:
        return Duration.ofMinutes(5);
      case SUBSCRIPTION_INTERVAL_10_MIN:
        return Duration.ofMinutes(10);
      case SUBSCRIPTION_INTERVAL_FIFTEEN_MINUTES:
        return Duration.ofMinutes(15);
      case SUBSCRIPTION_INTERVAL_30_MIN:
        return Duration.ofMinutes(30);
      case SUBSCRIPTION_INTERVAL_ONE_HOUR:
        return Duration.ofHours(1);
      case SUBSCRIPTION_INTERVAL_2_HOUR:
        return Duration.ofHours(2);
      case SUBSCRIPTION_INTERVAL_4_HOUR:
        return Duration.ofHours(4);
      case SUBSCRIPTION_INTERVAL_ONE_DAY:
        return Duration.ofDays(1);
      case SUBSCRIPTION_INTERVAL_WEEK:
        return Duration.ofDays(7);
      case SUBSCRIPTION_INTERVAL_MONTH:
        return Duration.between(periodStart, periodStart.plusMonths(1));
      default:
        throw new IllegalArgumentException("Unsupported interval");
    }
  }

  /**
   * <a href=https://russianinvestments.github.io/investAPI/load_history/>
   * О лимитах на загрузку свечей
   * </a>
   */
  public static LocalDateTime getStartTime(LocalDateTime endTime, SubscriptionInterval interval) {
    switch (interval) {
      case SUBSCRIPTION_INTERVAL_ONE_MINUTE:
      case SUBSCRIPTION_INTERVAL_2_MIN:
      case SUBSCRIPTION_INTERVAL_3_MIN:
        return endTime.minusDays(1);
      case SUBSCRIPTION_INTERVAL_FIVE_MINUTES:
      case SUBSCRIPTION_INTERVAL_10_MIN:
        return endTime.minusWeeks(1);
      case SUBSCRIPTION_INTERVAL_FIFTEEN_MINUTES:
      case SUBSCRIPTION_INTERVAL_30_MIN:
        return endTime.minusWeeks(3);
      case SUBSCRIPTION_INTERVAL_ONE_HOUR:
      case SUBSCRIPTION_INTERVAL_2_HOUR:
      case SUBSCRIPTION_INTERVAL_4_HOUR:
        return endTime.minusMonths(3);
      case SUBSCRIPTION_INTERVAL_ONE_DAY:
        return endTime.minusYears(6);
      case SUBSCRIPTION_INTERVAL_WEEK:
        return endTime.minusYears(5);
      case SUBSCRIPTION_INTERVAL_MONTH:
        return endTime.minusYears(10);
      default:
        throw new IllegalArgumentException("Unsupported interval");
    }
  }
}
