// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import java.util.List;

public record SuppressionSettings(Boolean inlineEnabled, List<String> keys) {
  private static final List<String> DEFAULT_KEYS =
      List.of(
          RuleIds.PRIMITIVE_BOXED_SIGNATURE,
          "java-evo-linter:" + RuleIds.PRIMITIVE_BOXED_SIGNATURE);

  public SuppressionSettings {
    inlineEnabled = inlineEnabled == null ? Boolean.TRUE : inlineEnabled;
    keys = keys == null || keys.isEmpty() ? DEFAULT_KEYS : List.copyOf(keys);
  }

  public SuppressionSettings() {
    this(Boolean.TRUE, DEFAULT_KEYS);
  }

  public SuppressionSettings withInlineEnabled(boolean value) {
    return new SuppressionSettings(value, keys);
  }

  public SuppressionSettings withKeys(List<String> value) {
    return new SuppressionSettings(inlineEnabled, value);
  }
}
