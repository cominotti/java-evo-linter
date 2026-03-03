// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

public final class OutputSettings {
  public OutputFormat format = OutputFormat.BOTH;
  public String jsonlPath = ".java-evo-linter-findings.jsonl";

  public OutputSettings copy() {
    var copy = new OutputSettings();
    copy.format = format;
    copy.jsonlPath = jsonlPath;
    return copy;
  }
}
