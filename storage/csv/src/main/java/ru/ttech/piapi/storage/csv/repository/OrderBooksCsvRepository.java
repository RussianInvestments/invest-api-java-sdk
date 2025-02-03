package ru.ttech.piapi.storage.csv.repository;

import com.google.common.base.Splitter;
import io.vavr.collection.Stream;
import org.apache.commons.csv.CSVRecord;
import ru.tinkoff.piapi.contract.v1.Order;
import ru.tinkoff.piapi.contract.v1.OrderBook;
import ru.tinkoff.piapi.contract.v1.OrderBookType;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.storage.csv.config.CsvConfiguration;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OrderBooksCsvRepository extends CsvRepository<OrderBook> {

  public OrderBooksCsvRepository(CsvConfiguration configuration) throws IOException {
    super(configuration);
  }

  @Override
  protected String[] getHeaders() {
    return new String[]{"time", "instrument_uid", "bids_prices", "bids_vols", "asks_prices", "asks_vols",
      "is_consistent", "depth", "limit_up", "limit_down", "order_book_type"};
  }

  @Override
  protected Iterable<?> convertToIterable(OrderBook entity) {
    return List.of(
      TimeMapper.timestampToLocalDateTime(entity.getTime()),
      entity.getInstrumentUid(),
      getValuesFromOrders(entity.getBidsList(), order -> NumberMapper.quotationToBigDecimal(order.getPrice())),
      getValuesFromOrders(entity.getBidsList(), Order::getQuantity),
      getValuesFromOrders(entity.getAsksList(), order -> NumberMapper.quotationToBigDecimal(order.getPrice())),
      getValuesFromOrders(entity.getAsksList(), Order::getQuantity),
      entity.getIsConsistent(),
      entity.getDepth(),
      NumberMapper.quotationToBigDecimal(entity.getLimitUp()),
      NumberMapper.quotationToBigDecimal(entity.getLimitDown()),
      entity.getOrderBookType().name()
    );
  }

  @Override
  protected OrderBook convertToEntity(CSVRecord csvRecord) {
    return OrderBook.newBuilder()
      .setTime(TimeMapper.localDateTimeToTimestamp(LocalDateTime.parse(csvRecord.get("time"))))
      .setInstrumentUid(csvRecord.get("instrument_uid"))
      .addAllBids(buildOrders(csvRecord.get("bids_prices"), csvRecord.get("bids_vols")))
      .addAllAsks(buildOrders(csvRecord.get("asks_prices"), csvRecord.get("asks_vols")))
      .setIsConsistent(Boolean.parseBoolean(csvRecord.get("is_consistent")))
      .setDepth(Integer.parseInt(csvRecord.get("depth")))
      .setLimitUp(NumberMapper.bigDecimalToQuotation(new BigDecimal(csvRecord.get("limit_up"))))
      .setLimitDown(NumberMapper.bigDecimalToQuotation(new BigDecimal(csvRecord.get("limit_down"))))
      .setOrderBookType(OrderBookType.valueOf(csvRecord.get("order_book_type")))
      .build();
  }

  private Iterable<Order> buildOrders(String pricesArray, String volsArray) {
    var prices = parseValues(pricesArray, BigDecimal::new);
    var quantities = parseValues(volsArray, Long::parseLong);
    return prices.zip(quantities).map(order -> buildOrder(order._1(), order._2()))
      .collect(Collectors.toUnmodifiableList());
  }

  private Order buildOrder(BigDecimal price, Long quantity) {
    return Order.newBuilder()
      .setPrice(NumberMapper.bigDecimalToQuotation(price))
      .setQuantity(quantity)
      .build();
  }

  private Iterable<?> getValuesFromOrders(List<Order> orders, Function<Order, ?> mapper) {
    return orders.stream().map(mapper).collect(Collectors.toUnmodifiableList());
  }

  private <T> Stream<T> parseValues(String array, Function<String, T> convertor) {
    return Stream.ofAll(parseElements(array)).map(convertor);
  }

  private Iterable<String> parseElements(String array) {
    return Splitter.on(", ").split(array.substring(1, array.length() - 1));
  }
}
