package ru.tinkoff.piapi.core.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.tinkoff.piapi.contract.v1.Quotation;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * value with mapping Quotation
 */
@AllArgsConstructor
@Getter
public class Quantity {
  public static final BigDecimal NANOS_MULTIPLIER = BigDecimal.valueOf(1_000_000_000L);
  public static final Quantity ZERO = new Quantity(BigDecimal.ZERO);
  public static final Quantity ONE = new Quantity(BigDecimal.ONE);
  private final BigDecimal value;

  public static Quantity ofQuotation(Quotation quotation) {
    return ofUnits(quotation.getUnits())
      .add(BigDecimal.valueOf(quotation.getNano(), 9));
  }

  public static Quantity ofUnits(long units) {
    if (units == 0) return ZERO;
    if (units == 1) return ONE;
    return new Quantity(BigDecimal.valueOf(units));
  }

  public Quantity add(BigDecimal value) {
    return new Quantity(this.value.add(value));
  }

  public Quantity subtract(BigDecimal value) {
    return new Quantity(this.value.subtract(value));
  }

  public Quantity add(Quantity quantity) {
    return new Quantity(this.value.add(quantity.value));
  }

  public Quantity subtract(Quantity quantity) {
    return new Quantity(this.value.subtract(quantity.value));
  }

  public Quantity mapValue(Function<BigDecimal, BigDecimal> mapper) {
    return new Quantity(mapper.apply(this.value));
  }

  public Quotation toQuotation() {
    return Quotation.newBuilder()
      .setUnits(value.longValue())
      .setNano(value.remainder(BigDecimal.ONE).multiply(NANOS_MULTIPLIER).intValue())
      .build();
  }

}
