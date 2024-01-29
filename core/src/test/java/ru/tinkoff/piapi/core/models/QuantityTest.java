package ru.tinkoff.piapi.core.models;

import org.junit.jupiter.api.Test;
import ru.tinkoff.piapi.contract.v1.Quotation;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QuantityTest {

  static BigDecimal FOUR = BigDecimal.valueOf(4);
  static BigDecimal FOUR_TWO = FOUR.add(new BigDecimal("0.2"));

  @Test
  void testAdd() {
    assertEquals(0,
      FOUR_TWO.compareTo(
        Quantity.ofUnits(1)
          .add(BigDecimal.ONE)
          .add(Quantity.ofQuotation(
            Quantity.ofQuotation(Quotation.newBuilder().setUnits(1).build())
              .toQuotation()
          ))
          .add(Quantity.ofQuotation(
            Quantity.ofQuotation(Quotation.newBuilder().setUnits(1).setNano(200_000_000).build())
              .toQuotation()
          ))
          .getValue()
      )
    );
  }

  @Test
  void testSubtract() {
    assertEquals(0,
      FOUR_TWO.compareTo(
        new Quantity(FOUR_TWO)
          .mapValue(value -> value.multiply(BigDecimal.valueOf(2)))
          .subtract(FOUR)
          .subtract(Quantity.ofQuotation(Quotation.newBuilder().setUnits(0).setNano(200_000_000).build()))
          .getValue())
    );
  }
}
