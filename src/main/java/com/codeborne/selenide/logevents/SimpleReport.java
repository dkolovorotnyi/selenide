package com.codeborne.selenide.logevents;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;
import org.slf4j.helpers.NOPLoggerFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.OptionalInt;

/**
 * A simple text report of Selenide actions performed during test run.
 *
 * Class is thread-safe: the same instance of SimpleReport can be reused by different threads simultaneously.
 */
@ParametersAreNonnullByDefault
public class SimpleReport {
  private static final Logger log = LoggerFactory.getLogger(SimpleReport.class);

  public void start() {
    checkThatSlf4jIsConfigured();
    SelenideLogger.addListener("simpleReport", new EventsCollector());
  }

  public void finish(String title) {
    EventsCollector logEventListener = SelenideLogger.removeListener("simpleReport");

    if (logEventListener == null) {
      log.warn("Can not publish report because Selenide logger has not started.");
      return;
    }

    String report = generateReport(title, logEventListener.events());
    log.info(report);
  }

  @Nonnull
  @CheckReturnValue
  String generateReport(String title, List<LogEvent> events) {
    OptionalInt maxLineLength = events
            .stream()
            .map(LogEvent::getElement)
            .map(String::length)
            .mapToInt(Integer::intValue)
            .max();

    int count = maxLineLength.orElse(0) >= 20 ? (maxLineLength.getAsInt()) : 20;

    StringBuilder sb = new StringBuilder();
    sb.append("Report for ").append(title).append('\n');

    String delimiter = '+' + String.join("+", line(count + 2), line(70 + 2), line(10 + 2), line(10 + 2)) + "+\n";

    sb.append(delimiter);
    sb.append(String.format("| %-" + count + "s | %-70s | %-10s | %-10s |%n", "Element", "Subject", "Status", "ms."));
    sb.append(delimiter);

    for (LogEvent e : events) {
      sb.append(String.format("| %-" + count + "s | %-70s | %-10s | %-10s |%n", e.getElement(), e.getSubject(),
              e.getStatus(), e.getDuration()));
    }
    sb.append(delimiter);
    return sb.toString();
  }

  public void clean() {
    SelenideLogger.removeListener("simpleReport");
  }

  @CheckReturnValue
  @Nonnull
  private String line(int count) {
    return String.join("", Collections.nCopies(count, "-"));
  }

  private static void checkThatSlf4jIsConfigured() {
    ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
    if (loggerFactory instanceof NOPLoggerFactory || loggerFactory.getLogger("com.codeborne.selenide") instanceof NOPLogger) {
      throw new IllegalStateException("SLF4J is not configured. You will not see any Selenide logs. \n" +
        "  Please add slf4j-simple.jar, slf4j-log4j12.jar or logback-classic.jar to your classpath. \n" +
        "  See https://github.com/selenide/selenide/wiki/slf4j");
    }
  }
}
