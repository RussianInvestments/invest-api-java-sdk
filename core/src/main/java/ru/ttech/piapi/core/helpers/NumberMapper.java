package ru.ttech.piapi.core.helpers;

import ru.tinkoff.piapi.contract.v1.Quotation;

import java.math.BigDecimal;

/**
 * Маппер для конвертации числовых объектов
 */
public class NumberMapper {

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
    if (value == null) {
      return null;
    }
    return mapUnitsAndNanos(value.getUnits(), value.getNano());
  }

  private static BigDecimal mapUnitsAndNanos(long units, int nanos) {
    if (units == 0 && nanos == 0) {
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf(units).add(BigDecimal.valueOf(nanos, 9));
  }
}
