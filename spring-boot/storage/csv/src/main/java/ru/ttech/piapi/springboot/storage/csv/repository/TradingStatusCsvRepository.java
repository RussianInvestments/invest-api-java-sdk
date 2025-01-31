package ru.ttech.piapi.springboot.storage.csv.repository;

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
    return new String[]{"time", "instrument_uid", "trading_status",
      "limit_order_available_flag", "market_order_available_flag"};
  }

  @Override
  protected Iterable<Object> convertToIterable(TradingStatus entity) {
    return List.of(
      TimeMapper.timestampToLocalDateTime(entity.getTime()),
      entity.getInstrumentUid(),
      entity.getTradingStatus(),
      entity.getLimitOrderAvailableFlag(),
      entity.getMarketOrderAvailableFlag()
    );
  }
}
