// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.cli;

import dev.cominotti.java.evo.linter.core.CheckReport;
import dev.cominotti.java.evo.linter.core.LinterConfig;
import dev.cominotti.java.evo.linter.core.LinterConfigLoader;
import dev.cominotti.java.evo.linter.core.LinterConfigOverrides;
import dev.cominotti.java.evo.linter.core.LinterEngine;
import dev.cominotti.java.evo.linter.core.LinterException;
import dev.cominotti.java.evo.linter.core.OutputFormat;
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
    var engine = new LinterEngine();
    var report = engine.check(root, effectiveConfig);

    renderHumanReport(engine, effectiveConfig.output().format(), report);
    renderJsonlReport(
        engine,
        effectiveConfig.output().format(),
        effectiveConfig.output().jsonlPath(),
        root,
        report);
    return report.newFindings().isEmpty() ? 0 : 1;
  }

  private LinterConfig applyOutputOverrides(LinterConfig baseConfig, Path projectRoot) {
    var overrides =
        new LinterConfigOverrides().withOutputFormat(outputFormat).withJsonlPath(jsonlPath);
    return new LinterConfigLoader().applyOverrides(baseConfig, overrides, projectRoot);
  }

  private void renderHumanReport(LinterEngine engine, OutputFormat format, CheckReport report) {
    if (format == OutputFormat.HUMAN || format == OutputFormat.BOTH) {
      out().println(engine.renderHumanReport(report));
    }
  }

  private void renderJsonlReport(
      LinterEngine engine,
      OutputFormat format,
      String jsonlPathValue,
      Path projectRoot,
      CheckReport report)
      throws LinterException {
    if (format != OutputFormat.JSONL && format != OutputFormat.BOTH) {
      return;
    }
    var outputPath = resolveProjectPath(projectRoot, Path.of(jsonlPathValue));
    engine.writeJsonlReport(outputPath, report.newFindings());
    if (format == OutputFormat.BOTH) {
      out().println("\nJSONL report written to " + outputPath);
    }
  }

  private Path resolveProjectPath(Path projectRoot, Path value) {
    if (value.isAbsolute()) {
      return value.normalize();
    }
    return projectRoot.resolve(value).normalize();
  }
}
