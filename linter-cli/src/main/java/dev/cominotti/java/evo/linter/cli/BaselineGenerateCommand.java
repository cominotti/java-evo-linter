// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.cli;

import dev.cominotti.java.evo.linter.core.LinterEngine;
import dev.cominotti.java.evo.linter.core.LinterException;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

@Command(
    name = "generate",
    description = "Generate or refresh baseline from current active findings",
    mixinStandardHelpOptions = true)
public final class BaselineGenerateCommand extends BaseCommand implements Callable<Integer> {
  @Override
  public Integer call() {
    try {
      var root = normalizedProjectRoot();
      var effectiveConfig = loadEffectiveConfig();

      var engine = new LinterEngine();
      var report = engine.generateBaseline(root, effectiveConfig);

      out().println("Baseline generated");
      out().println("  path: " + report.baselinePath());
      out().println("  entries: " + report.entries().size());
      out().println("  active_findings: " + report.activeFindingCount());
      out().println("  inline_suppressed: " + report.inlineSuppressedCount());
      return 0;
    } catch (LinterException exception) {
      err().println("java-evo-linter: " + exception.getMessage());
      return 2;
    } catch (Exception exception) {
      err().println("java-evo-linter: unexpected error: " + exception.getMessage());
      return 2;
    }
  }
}
