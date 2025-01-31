package ru.ttech.piapi.springboot.storage.csv.repository;

import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.csv.config.CsvConfiguration;

import java.io.IOException;
import java.util.List;

public class LastPricesCsvRepository extends CsvRepository<LastPrice> {

  public LastPricesCsvRepository(CsvConfiguration configuration) throws IOException {
    super(configuration);
  }

  @Override
  protected String[] getHeaders() {
    return new String[] {"time", "instrument_uid", "price", "last_price_type"};
  }

  @Override
  protected Iterable<Object> convertToIterable(LastPrice entity) {
    return List.of(
      TimeMapper.timestampToLocalDateTime(entity.getTime()),
      entity.getInstrumentUid(),
      NumberMapper.quotationToBigDecimal(entity.getPrice()),
      entity.getLastPriceType()
    );
  }
}
