package io.cominotti.javaevo.linter.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum OutputFormat {
  HUMAN,
  JSONL,
  BOTH;

  @JsonCreator
  public static OutputFormat fromString(String raw) {
    var normalized = raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case null -> BOTH;
      case "" -> BOTH;
      case "human" -> HUMAN;
      case "jsonl" -> JSONL;
      case "both" -> BOTH;
      default -> throw new IllegalArgumentException("Unsupported output format: " + raw);
    };
  }

  @JsonValue
  public String toValue() {
    return name().toLowerCase(Locale.ROOT);
  }
}
