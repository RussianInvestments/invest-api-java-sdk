package ru.ttech.piapi.example.cmd.history;

import io.vavr.Tuple;
import picocli.CommandLine;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.ttech.piapi.example.cmd.MainCommand;
import ru.ttech.piapi.example.cmd.strategy.AvailableCandleIntervals;
import ru.ttech.piapi.example.strategy.backtest.BarsLoadingExample;

import java.time.LocalDate;
import java.util.List;

@CommandLine.Command(
  name = "history-bars",
  mixinStandardHelpOptions = true,
  description = "Загрузка исторических свечей по инструменту"
)
public class BarsLoadingCommand implements Runnable {

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
    description = "Интервал свечи. Возможные значения: ${COMPLETION-CANDIDATES}",
    completionCandidates = AvailableCandleIntervals.class,
    required = true
  )
  private CandleInterval candleInterval;

  @CommandLine.Option(names = {"--start-date", "-s"}, description = "Дата с которой необходимо загрузить свечи", required = true)
  private LocalDate from;

  @Override
  public void run() {
    var barsLoader = new BarsLoadingExample();
    barsLoader.loadBars(parent.getConfiguration(), List.of(Tuple.of(instrumentId, candleInterval)), from);
  }
}
