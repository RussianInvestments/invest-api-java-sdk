package ru.ttech.piapi.springboot.storage.csv.repository;

import com.google.protobuf.Descriptors;
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
    // TODO: как-нибудь параметризировать
    return LastPrice.getDescriptor().getFields().stream()
      .map(Descriptors.FieldDescriptor::getName)
      .toArray(String[]::new);
  }

  @Override
  protected Iterable<Object> convertToIterable(LastPrice entity) {
    return List.of(
      entity.getFigi(),
      NumberMapper.quotationToBigDecimal(entity.getPrice()),
      TimeMapper.timestampToLocalDate(entity.getTime()),
      entity.getInstrumentUid(),
      entity.getLastPriceType()
    );
  }
}
