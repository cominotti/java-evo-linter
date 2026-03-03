// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.cli;

import io.cominotti.javaevo.linter.core.HumanReporter;
import io.cominotti.javaevo.linter.core.JsonlReporter;
import io.cominotti.javaevo.linter.core.LinterConfigLoader;
import io.cominotti.javaevo.linter.core.LinterConfigOverrides;
import io.cominotti.javaevo.linter.core.LinterEngine;
import io.cominotti.javaevo.linter.core.LinterException;
import io.cominotti.javaevo.linter.core.OutputFormat;
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
      var root = normalizedProjectRoot();

      var configLoader = new LinterConfigLoader();
      var config = configLoader.load(configPath, root);

      var overrides = new LinterConfigOverrides();
      overrides.baselinePath = baselinePath;
      overrides.outputFormat = outputFormat;
      overrides.jsonlPath = jsonlPath;
      overrides.sourceRoots = sourceRoots;
      overrides.classpathEntries = classpathEntries;

      if (disablePrivate || disablePrivateFields) {
        overrides.includePrivateFields = false;
      }
      if (disablePackagePrivate || disablePackagePrivateFields) {
        overrides.includePackagePrivateFields = false;
      }
      if (disablePrivate || disablePrivateMethods) {
        overrides.includePrivateMethods = false;
      }
      if (disablePackagePrivate || disablePackagePrivateMethods) {
        overrides.includePackagePrivateMethods = false;
      }

      var effectiveConfig = configLoader.applyOverrides(config, overrides, root);

      var engine = new LinterEngine();
      var report = engine.check(root, effectiveConfig);

      var humanReporter = new HumanReporter();
      var jsonlReporter = new JsonlReporter();

      if (effectiveConfig.output.format == OutputFormat.HUMAN
          || effectiveConfig.output.format == OutputFormat.BOTH) {
        System.out.println(humanReporter.renderCheckReport(report));
      }

      if (effectiveConfig.output.format == OutputFormat.JSONL
          || effectiveConfig.output.format == OutputFormat.BOTH) {
        Path outputPath =
            LinterConfigLoader.normalizePath(root, Path.of(effectiveConfig.output.jsonlPath));
        jsonlReporter.writeToPath(outputPath, report.newFindings());
        if (effectiveConfig.output.format == OutputFormat.BOTH) {
          System.out.println("\nJSONL report written to " + outputPath);
        }
      }

      return report.newFindings().isEmpty() ? 0 : 1;
    } catch (LinterException exception) {
      System.err.println("java-evo-linter: " + exception.getMessage());
      return 2;
    } catch (Exception exception) {
      System.err.println("java-evo-linter: unexpected error: " + exception.getMessage());
      return 2;
    }
  }
}
