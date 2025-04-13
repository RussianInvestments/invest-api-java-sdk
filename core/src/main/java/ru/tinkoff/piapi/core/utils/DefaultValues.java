package ru.tinkoff.piapi.core.utils;

import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;

/**
 * Containing constants with default values to be imported where needed.
 */
@Deprecated(since = "1.30", forRemoval = true)
public class DefaultValues {

  public static final SubscriptionInterval DEFAULT_SUBSCRIPTION_INTERVAL = SubscriptionInterval.SUBSCRIPTION_INTERVAL_ONE_MINUTE;

}
