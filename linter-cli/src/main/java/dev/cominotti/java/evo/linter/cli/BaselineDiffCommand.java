// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.cli;

import dev.cominotti.java.evo.linter.core.LinterEngine;
import dev.cominotti.java.evo.linter.core.LinterException;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "diff",
    description = "Compare current findings against baseline and report missing/stale entries",
    mixinStandardHelpOptions = true)
public final class BaselineDiffCommand extends BaseCommand implements Callable<Integer> {
  @Option(
      names = "--fail-on-stale",
      description = "Return non-zero when stale baseline entries are found")
  boolean failOnStale;

  @Override
  public Integer call() {
    try {
      var root = normalizedProjectRoot();
      var effectiveConfig = loadEffectiveConfig();

      var engine = new LinterEngine();
      var report = engine.diffBaseline(root, effectiveConfig);

      out().println(engine.renderHumanReport(report));

      var fail =
          !report.newFindings().isEmpty() || (failOnStale && !report.staleEntries().isEmpty());
      return fail ? 1 : 0;
    } catch (LinterException exception) {
      err().println("java-evo-linter: " + exception.getMessage());
      return 2;
    } catch (Exception exception) {
      err().println("java-evo-linter: unexpected error: " + exception.getMessage());
      return 2;
    }
  }
}
