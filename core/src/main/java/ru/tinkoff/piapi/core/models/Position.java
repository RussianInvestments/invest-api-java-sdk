package ru.tinkoff.piapi.core.models;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import ru.tinkoff.piapi.contract.v1.PortfolioPosition;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode
@Builder
public class Position {
  private final String figi;
  private final String instrumentType;
  private final BigDecimal quantity;
  private final Money averagePositionPrice;
  private final BigDecimal expectedYield;
  private final Money currentNkd;
  @Deprecated
  private final BigDecimal averagePositionPricePt;
  private final Money currentPrice;
  private final Money averagePositionPriceFifo;
  @Deprecated
  private final BigDecimal quantityLots;
  private final boolean blocked;
  private final BigDecimal blockedLots;
  private final String positionUid;
  private final String instrumentUid;
  private final Money varMargin;
  private final BigDecimal expectedYieldFifo;

  private Position(
    @Nonnull String figi,
    @Nonnull String instrumentType,
    @Nonnull BigDecimal quantity,
    @Nonnull Money averagePositionPrice,
    @Nonnull BigDecimal expectedYield,
    @Nonnull Money currentNkd,
    @Nonnull BigDecimal averagePositionPricePt,
    @Nonnull Money currentPrice,
    @Nonnull Money averagePositionPriceFifo,
    @Nonnull BigDecimal quantityLots,
    boolean blocked,
    @Nonnull BigDecimal blockedLots,
    @Nonnull String positionUid,
    @Nonnull String instrumentUid,
    @Nonnull Money varMargin,
    @Nonnull BigDecimal expectedYieldFifo
  ) {
    this.figi = figi;
    this.instrumentType = instrumentType;
    this.quantity = quantity;
    this.averagePositionPrice = averagePositionPrice;
    this.expectedYield = expectedYield;
    this.currentNkd = currentNkd;
    this.averagePositionPricePt = averagePositionPricePt;
    this.currentPrice = currentPrice;
    this.averagePositionPriceFifo = averagePositionPriceFifo;
    this.quantityLots = quantityLots;
    this.blocked = blocked;
    this.blockedLots = blockedLots;
    this.positionUid = positionUid;
    this.instrumentUid = instrumentUid;
    this.varMargin = varMargin;
    this.expectedYieldFifo = expectedYieldFifo;
  }

  @Nonnull
  public static Position fromResponse(@Nonnull PortfolioPosition portfolioPosition) {
    return new Position(
      portfolioPosition.getFigi(),
      portfolioPosition.getInstrumentType(),
      MapperUtils.quotationToBigDecimal(portfolioPosition.getQuantity()),
      Money.fromResponse(portfolioPosition.getAveragePositionPrice()),
      MapperUtils.quotationToBigDecimal(portfolioPosition.getExpectedYield()),
      Money.fromResponse(portfolioPosition.getCurrentNkd()),
      MapperUtils.quotationToBigDecimal(portfolioPosition.getAveragePositionPricePt()),
      Money.fromResponse(portfolioPosition.getCurrentPrice()),
      Money.fromResponse(portfolioPosition.getAveragePositionPriceFifo()),
      MapperUtils.quotationToBigDecimal(portfolioPosition.getQuantityLots()),
      portfolioPosition.getBlocked(),
      MapperUtils.quotationToBigDecimal(portfolioPosition.getBlockedLots()),
      portfolioPosition.getPositionUid(),
      portfolioPosition.getInstrumentUid(),
      Money.fromResponse(portfolioPosition.getVarMargin()),
      MapperUtils.quotationToBigDecimal(portfolioPosition.getExpectedYieldFifo())
    );
  }

  public static List<Position> fromResponse(@Nonnull List<PortfolioPosition> portfolioPositions) {
    return portfolioPositions.stream().map(Position::fromResponse).collect(Collectors.toList());
  }
}
