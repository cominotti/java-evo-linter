// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

public record BaselineSettings(Boolean enabled, String path) {
  private static final String DEFAULT_PATH = ".java-evo-linter-baseline.jsonl";

  public BaselineSettings {
    enabled = enabled == null ? Boolean.TRUE : enabled;
    path = path == null || path.isBlank() ? DEFAULT_PATH : path;
  }

  public BaselineSettings() {
    this(Boolean.TRUE, DEFAULT_PATH);
  }

  public BaselineSettings withEnabled(boolean value) {
    return new BaselineSettings(value, path);
  }

  public BaselineSettings withPath(String value) {
    return new BaselineSettings(enabled, value);
  }
}
