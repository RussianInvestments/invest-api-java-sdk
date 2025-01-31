package ru.ttech.piapi.springboot.storage.csv.repository;

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
    return new String[]{"time", "instrument_uid", "direction", "price", "quantity", "trade_source"};
  }

  @Override
  protected Iterable<Object> convertToIterable(Trade entity) {
    return List.of(
      TimeMapper.timestampToLocalDateTime(entity.getTime()),
      entity.getInstrumentUid(),
      entity.getDirection(),
      entity.getPrice(),
      entity.getQuantity(),
      entity.getTradeSource()
    );
  }
}
