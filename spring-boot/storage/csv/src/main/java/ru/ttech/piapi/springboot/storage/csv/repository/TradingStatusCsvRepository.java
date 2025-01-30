package ru.ttech.piapi.springboot.storage.csv.repository;

import com.google.protobuf.Descriptors;
import ru.tinkoff.piapi.contract.v1.TradingStatus;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.csv.config.CsvConfiguration;

import java.io.IOException;
import java.util.List;

public class TradingStatusCsvRepository extends CsvRepository<TradingStatus> {

  public TradingStatusCsvRepository(CsvConfiguration configuration) throws IOException {
    super(configuration);
  }

  @Override
  protected String[] getHeaders() {
    // TODO: как-нибудь параметризировать
    return TradingStatus.getDescriptor().getFields().stream()
      .map(Descriptors.FieldDescriptor::getName)
      .toArray(String[]::new);
  }

  @Override
  protected Iterable<Object> convertToIterable(TradingStatus entity) {
    return List.of(
      entity.getFigi(),
      entity.getTradingStatus(),
      TimeMapper.timestampToLocalDateTime(entity.getTime()),
      entity.getLimitOrderAvailableFlag(),
      entity.getMarketOrderAvailableFlag(),
      entity.getInstrumentUid()
    );
  }
}
