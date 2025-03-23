package ru.ttech.piapi.example.cmd.strategy;


import picocli.CommandLine;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.ttech.piapi.example.cmd.MainCommand;
import ru.ttech.piapi.example.strategy.backtest.ChooseBestStrategyExample;

import java.time.LocalDate;

@CommandLine.Command(
  name = "backtest",
  mixinStandardHelpOptions = true,
  description = "Бэктест стратегии cross-EMA с разными параметрами на исторических данных"
)
public class BacktestStrategyCommand implements Runnable {

  @CommandLine.ParentCommand
  private MainCommand parent;

  @CommandLine.Option(
    names = {"--instrument-id", "-i"},
    description = "Индентификатор инструмента в формате UID",
    required = true
  )
  private String instrumentId;

  @CommandLine.Option(
    names = {"--candle-interval", "-c"},
    description = "Интервал свечи для бэктеста. Возможные значения: ${COMPLETION-CANDIDATES}",
    completionCandidates = AvailableCandleIntervals.class,
    required = true
  )
  private CandleInterval candleInterval;

  @CommandLine.Option(
    names = {"--short-ema-start"},
    defaultValue = "10",
    description = "Начальное значение перебора для короткой EMA",
    showDefaultValue = CommandLine.Help.Visibility.ALWAYS
  )
  private int shortEmaStart;

  @CommandLine.Option(
    names = {"--short-ema-end"},
    defaultValue = "15",
    description = "Конечное значение перебора для короткой EMA",
    showDefaultValue = CommandLine.Help.Visibility.ALWAYS
  )
  private int shortEmaEnd;

  @CommandLine.Option(
    names = {"--long-ema-start"},
    defaultValue = "10",
    description = "Начальное значение перебора для длинной EMA",
    showDefaultValue = CommandLine.Help.Visibility.ALWAYS
  )
  private int longEmaStart;

  @CommandLine.Option(
    names = {"--long-ema-end"},
    defaultValue = "15",
    description = "Конечное значение перебора для длинной EMA",
    showDefaultValue = CommandLine.Help.Visibility.ALWAYS
  )
  private int longEmaEnd;

  @CommandLine.Option(
    names = {"--commission-fee"},
    defaultValue = "0.0005",
    description = "Комиссия за сделку",
    showDefaultValue = CommandLine.Help.Visibility.ALWAYS
  )
  private double commissionFee;

  @CommandLine.Option(names = {"--start-date", "-s"}, description = "Дата начала бэктеста", required = true)
  private LocalDate from;

  @CommandLine.Option(names = {"--end-date", "-e"}, description = "Дата окончания бэктеста", required = true)
  private LocalDate to;

  @Override
  public void run() {
    ChooseBestStrategyExample chooseBestStrategyExample = new ChooseBestStrategyExample();
    var configuration = parent.getConfiguration();
    chooseBestStrategyExample.startBacktest(configuration, instrumentId, candleInterval, from, to,
      shortEmaStart, shortEmaEnd, longEmaStart, longEmaEnd, commissionFee);
  }
}
