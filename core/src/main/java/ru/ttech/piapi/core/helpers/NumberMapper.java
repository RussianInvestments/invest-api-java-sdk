package ru.ttech.piapi.core.helpers;

import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.Quotation;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;

/**
 * Маппер для конвертации числовых объектов
 */
public class NumberMapper {

  private static final Function<Quotation, BigDecimal> convert = val -> mapUnitsAndNanos(val.getUnits(), val.getNano());
  private static final Function<MoneyValue, BigDecimal> convertMoney = val -> mapUnitsAndNanos(val.getUnits(), val.getNano());

  /**
   * Конвертирует Quotation в BigDecimal.
   * <p>Например:<pre>{@code
   * {units: 10, nanos: 900000000}  --->  10.9
   * }</pre>
   *
   * @param value значение в формате Quotation
   * @return Значение в формате BigDecimal
   */
  public static BigDecimal quotationToBigDecimal(Quotation value) {
    return Optional.ofNullable(value)
      .map(convert)
      .orElseGet(() -> convert.apply(Quotation.getDefaultInstance()));
  }

  /**
   * Конвертирует MoneyValue в BigDecimal.
   * <p>Например:<pre>{@code
   * {currency: rub, units: 10, nanos: 900000000}  --->  10.9
   * }</pre>
   *
   * @param value значение в формате MoneyValue
   * @return Значение в формате BigDecimal
   */
  public static BigDecimal moneyValueToBigDecimal(MoneyValue value) {
    return Optional.ofNullable(value)
      .map(convertMoney)
      .orElseGet(() -> convertMoney.apply(MoneyValue.getDefaultInstance()));
  }

  /**
   * Конвертирует BigDecimal в Quotation.
   * <p>Например:<pre>{@code
   * 10.9 ---> {units: 10, nanos: 900000000}
   * }</pre>
   *
   * @param value значение в формате BigDecimal
   * @return Значение в формате  Quotation
   */
  public static Quotation bigDecimalToQuotation(BigDecimal value) {
    return Optional.ofNullable(value)
      .map(val -> Quotation.newBuilder()
        .setUnits(val.longValue())
        .setNano(val.remainder(BigDecimal.ONE).movePointRight(9).abs().intValue())
        .build())
      .orElse(Quotation.getDefaultInstance());
  }

  /**
   * Конвертирует BigDecimal в MoneyValue.
   * <p>Например:<pre>{@code
   * 10.9, rub ---> {currency: rub, units: 10, nanos: 900000000}
   * }</pre>
   *
   * @param value значение в формате BigDecimal
   * @return Значение в формате MoneyValue
   */
  public static MoneyValue bigDecimalToMoneyValue(BigDecimal value, String currency) {
    return Optional.ofNullable(value)
      .map(val -> MoneyValue.newBuilder()
        .setCurrency(currency)
        .setUnits(val.longValue())
        .setNano(val.remainder(BigDecimal.ONE).movePointRight(9).abs().intValue())
        .build())
      .orElse(MoneyValue.getDefaultInstance());
  }

  private static BigDecimal mapUnitsAndNanos(long units, int nanos) {
    if (units == 0 && nanos == 0) {
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf(units).add(BigDecimal.valueOf(nanos, 9));
  }
}
