package io.cominotti.javaevo.linter.cli;

import io.cominotti.javaevo.linter.core.LinterEngine;
import io.cominotti.javaevo.linter.core.LinterException;
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

      System.out.println("Baseline generated");
      System.out.println("  path: " + report.baselinePath());
      System.out.println("  entries: " + report.entries().size());
      System.out.println("  active_findings: " + report.activeFindingCount());
      System.out.println("  inline_suppressed: " + report.inlineSuppressedCount());
      return 0;
    } catch (LinterException exception) {
      System.err.println("java-evo-linter: " + exception.getMessage());
      return 2;
    } catch (Exception exception) {
      System.err.println("java-evo-linter: unexpected error: " + exception.getMessage());
      return 2;
    }
  }
}
