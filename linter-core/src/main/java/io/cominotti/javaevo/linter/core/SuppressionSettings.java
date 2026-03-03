// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import java.util.ArrayList;
import java.util.List;

public final class SuppressionSettings {
  public boolean inlineEnabled = true;
  public List<String> keys =
      new ArrayList<>(
          List.of(
              RuleIds.PRIMITIVE_BOXED_SIGNATURE,
              "java-evo-linter:" + RuleIds.PRIMITIVE_BOXED_SIGNATURE));

  public SuppressionSettings copy() {
    var copy = new SuppressionSettings();
    copy.inlineEnabled = inlineEnabled;
    copy.keys = keys == null ? new ArrayList<>() : new ArrayList<>(keys);
    return copy;
  }
}
