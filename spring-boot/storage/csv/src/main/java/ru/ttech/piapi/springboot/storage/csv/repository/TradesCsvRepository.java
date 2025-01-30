package ru.ttech.piapi.springboot.storage.csv.repository;

import com.google.protobuf.Descriptors;
import ru.tinkoff.piapi.contract.v1.Trade;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.csv.config.CsvConfiguration;

import java.io.IOException;
import java.util.List;

public class TradesCsvRepository extends CsvRepository<Trade> {

  public TradesCsvRepository(CsvConfiguration configuration) throws IOException {
    super(configuration);
  }

  @Override
  protected String[] getHeaders() {
    // TODO: как-нибудь параметризировать
    return Trade.getDescriptor().getFields().stream()
      .map(Descriptors.FieldDescriptor::getName)
      .toArray(String[]::new);
  }

  @Override
  protected Iterable<Object> convertToIterable(Trade entity) {
    return List.of(
      entity.getFigi(),
      entity.getDirection(),
      entity.getPrice(),
      entity.getQuantity(),
      TimeMapper.timestampToLocalDateTime(entity.getTime()),
      entity.getInstrumentUid(),
      entity.getTradeSource()
    );
  }
}
