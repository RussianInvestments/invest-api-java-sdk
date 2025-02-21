package ru.ttech.piapi.springboot.example.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "trading.bot")
public class TradingProperties {
  /**
   * Количество лотов одного инструмента, которыми будет торговать бот
   */
  private long lots;
  /**
  * Баланс счёта песочницы
   */
  private long balance;
}
