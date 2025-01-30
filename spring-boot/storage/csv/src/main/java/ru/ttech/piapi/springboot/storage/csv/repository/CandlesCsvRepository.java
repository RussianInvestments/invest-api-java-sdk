package ru.ttech.piapi.springboot.storage.csv.repository;

import com.google.protobuf.Descriptors;
import ru.tinkoff.piapi.contract.v1.Candle;
import ru.ttech.piapi.core.helpers.NumberMapper;
import ru.ttech.piapi.core.helpers.TimeMapper;
import ru.ttech.piapi.springboot.storage.csv.config.CsvConfiguration;

import java.io.IOException;
import java.util.List;

public class CandlesCsvRepository extends CsvRepository<Candle> {

  public CandlesCsvRepository(CsvConfiguration configuration) throws IOException {
    super(configuration);
  }
  @Override
  protected String[] getHeaders() {
    // TODO: как-нибудь параметризировать
    return Candle.getDescriptor().getFields().stream()
      .map(Descriptors.FieldDescriptor::getName)
      .toArray(String[]::new);
  }

  @Override
  public Iterable<Object> convertToIterable(Candle candle) {
    return List.of(
      candle.getFigi(),
      candle.getInterval(),
      NumberMapper.quotationToBigDecimal(candle.getOpen()),
      NumberMapper.quotationToBigDecimal(candle.getHigh()),
      NumberMapper.quotationToBigDecimal(candle.getLow()),
      NumberMapper.quotationToBigDecimal(candle.getClose()),
      candle.getVolume(),
      TimeMapper.timestampToLocalDateTime(candle.getTime()),
      TimeMapper.timestampToLocalDateTime(candle.getLastTradeTs()),
      candle.getInstrumentUid(),
      candle.getCandleSourceType()
    );
  }
}
