// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

public final class BaselineSettings {
  public boolean enabled = true;
  public String path = ".java-evo-linter-baseline.jsonl";

  public BaselineSettings copy() {
    var copy = new BaselineSettings();
    copy.enabled = enabled;
    copy.path = path;
    return copy;
  }
}
