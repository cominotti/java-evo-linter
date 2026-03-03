// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

public record OutputSettings(OutputFormat format, String jsonlPath) {
  private static final String DEFAULT_JSONL_PATH = ".java-evo-linter-findings.jsonl";

  public OutputSettings {
    format = format == null ? OutputFormat.BOTH : format;
    jsonlPath = jsonlPath == null || jsonlPath.isBlank() ? DEFAULT_JSONL_PATH : jsonlPath;
  }

  public OutputSettings() {
    this(OutputFormat.BOTH, DEFAULT_JSONL_PATH);
  }

  public OutputSettings withFormat(OutputFormat value) {
    return new OutputSettings(value, jsonlPath);
  }

  public OutputSettings withJsonlPath(String value) {
    return new OutputSettings(format, value);
  }
}
