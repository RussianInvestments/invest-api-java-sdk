package ru.ttech.piapi.strategy.candle.backtest;

public class BarData {

  private final String startTime;
  private final double open;
  private final double high;
  private final double low;
  private final double close;
  private final long volume;

  public BarData(String startTime, double open, double high, double low, double close, long volume) {
    this.startTime = startTime;
    this.open = open;
    this.high = high;
    this.low = low;
    this.close = close;
    this.volume = volume;
  }

  public String getStartTime() {
    return startTime;
  }

  public double getOpen() {
    return open;
  }

  public double getHigh() {
    return high;
  }

  public double getLow() {
    return low;
  }

  public double getClose() {
    return close;
  }

  public long getVolume() {
    return volume;
  }
}
