// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.cli;

import io.cominotti.javaevo.linter.core.HumanReporter;
import io.cominotti.javaevo.linter.core.JsonlReporter;
import io.cominotti.javaevo.linter.core.LinterConfig;
import io.cominotti.javaevo.linter.core.LinterConfigLoader;
import io.cominotti.javaevo.linter.core.LinterConfigOverrides;
import io.cominotti.javaevo.linter.core.LinterEngine;
import io.cominotti.javaevo.linter.core.LinterException;
import io.cominotti.javaevo.linter.core.OutputFormat;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "check",
    description = "Scan production code and report primitive/boxed signature findings",
    mixinStandardHelpOptions = true)
public final class CheckCommand extends BaseCommand implements Callable<Integer> {
  @Option(
      names = "--format",
      description = "Output format: ${COMPLETION-CANDIDATES}",
      converter = OutputFormatConverter.class)
  @Nullable OutputFormat outputFormat;

  @Option(names = "--jsonl-path", description = "Path to JSONL output file")
  @Nullable Path jsonlPath;

  @Override
  public Integer call() {
    try {
      return runCheck();
    } catch (LinterException exception) {
      err().println("java-evo-linter: " + exception.getMessage());
      return 2;
    } catch (IOException exception) {
      err().println("java-evo-linter: " + exception.getMessage());
      return 2;
    } catch (Exception exception) {
      err().println("java-evo-linter: unexpected error: " + exception.getMessage());
      return 2;
    }
  }

  private Integer runCheck() throws LinterException, IOException {
    var root = normalizedProjectRoot();
    var effectiveConfig = applyOutputOverrides(loadEffectiveConfig(), root);
    var report = new LinterEngine().check(root, effectiveConfig);

    renderHumanReport(effectiveConfig.output().format(), report);
    renderJsonlReport(
        effectiveConfig.output().format(), effectiveConfig.output().jsonlPath(), root, report);
    return report.newFindings().isEmpty() ? 0 : 1;
  }

  private LinterConfig applyOutputOverrides(LinterConfig baseConfig, Path projectRoot) {
    var overrides =
        new LinterConfigOverrides().withOutputFormat(outputFormat).withJsonlPath(jsonlPath);
    return new LinterConfigLoader().applyOverrides(baseConfig, overrides, projectRoot);
  }

  private void renderHumanReport(
      OutputFormat format, io.cominotti.javaevo.linter.core.CheckReport report) {
    if (format == OutputFormat.HUMAN || format == OutputFormat.BOTH) {
      out().println(new HumanReporter().renderCheckReport(report));
    }
  }

  private void renderJsonlReport(
      OutputFormat format,
      String jsonlPathValue,
      Path projectRoot,
      io.cominotti.javaevo.linter.core.CheckReport report)
      throws LinterException {
    if (format != OutputFormat.JSONL && format != OutputFormat.BOTH) {
      return;
    }
    var outputPath = LinterConfigLoader.normalizePath(projectRoot, Path.of(jsonlPathValue));
    new JsonlReporter().writeToPath(outputPath, report.newFindings());
    if (format == OutputFormat.BOTH) {
      out().println("\nJSONL report written to " + outputPath);
    }
  }
}
