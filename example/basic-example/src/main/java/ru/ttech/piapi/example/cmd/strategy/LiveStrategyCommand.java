package ru.ttech.piapi.example.cmd.strategy;

import picocli.CommandLine;
import ru.tinkoff.piapi.contract.v1.CandleInstrument;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.SubscriptionInterval;
import ru.ttech.piapi.example.cmd.MainCommand;
import ru.ttech.piapi.example.strategy.live.LiveCandleStrategyExample;
import ru.ttech.piapi.example.strategy.live.TradingServiceExample;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@CommandLine.Command(
  name = "live-trading",
  mixinStandardHelpOptions = true,
  description = "Запуск стратегии cross-EMA на реальном рынке"
)
public class LiveStrategyCommand implements Runnable {

  @CommandLine.ParentCommand
  private MainCommand parent;

  @CommandLine.Option(
    names = {"--instrument-id", "-i"},
    description = "Индентификатор инструмента в формате UID",
    required = true
  )
  private String instrumentId;

  @CommandLine.Option(
    names = {"--candle-interval"},
    description = "Интервал свечи. Возможные значения: ${COMPLETION-CANDIDATES}",
    completionCandidates = AvailableCandleIntervals.class,
    required = true
  )
  private CandleInterval candleInterval;

  @CommandLine.Option(
    names = {"--warmup-length", "-w"},
    defaultValue = "100",
    description = "Количество свечей, которые будут загружены перед стартом стратегии. " +
      "Требуется для корректной работы индикаторов технического анализа",
    showDefaultValue = CommandLine.Help.Visibility.ALWAYS
  )
  private int warmupLength;

  @CommandLine.Option(
    names = {"--sandbox-balance", "-b"},
    defaultValue = "1000000",
    description = "Начальный баланс песочницы. " +
      "Чтобы торговать в песочнице, нужно в properties установить значение sandbox.enabled в 'true'",
    showDefaultValue = CommandLine.Help.Visibility.ALWAYS
  )
  private BigDecimal sandboxBalance;

  @CommandLine.Option(
    names = {"--instrument-lots", "-lots"},
    defaultValue = "1",
    description = "Количество лотов одного инструмента, на которые будет выставлена заявка " +
      "на покупку или продажу по сигналу стратегии",
    showDefaultValue = CommandLine.Help.Visibility.ALWAYS
  )
  private int instrumentLots;

  @CommandLine.Option(
    names = {"--short-ema", "-s"},
    defaultValue = "5",
    description = "Период короткой EMA",
    showDefaultValue = CommandLine.Help.Visibility.ALWAYS
  )
  private int shortEma;

  @CommandLine.Option(
    names = {"--long-ema", "-l"},
    defaultValue = "15",
    description = "Период длинной EMA",
    showDefaultValue = CommandLine.Help.Visibility.ALWAYS
  )
  private int longEma;

  @Override
  public void run() {
    var serviceFactory = parent.getFactory();
    var strategy = new LiveCandleStrategyExample();
    var tradingService = new TradingServiceExample(serviceFactory, sandboxBalance, Set.of(instrumentId), instrumentLots);
    var instrument = CandleInstrument.newBuilder()
      .setInstrumentId(instrumentId)
      .setInterval(SubscriptionInterval.forNumber(candleInterval.getNumber()))
      .build();
    strategy.startStrategy(parent.getFactory(), warmupLength,
      Map.of(instrument, LiveCandleStrategyExample.createStrategy(shortEma, longEma)), tradingService
    );
  }
}
