// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.cli;

import dev.cominotti.java.evo.linter.core.LinterConfig;
import dev.cominotti.java.evo.linter.core.LinterConfigLoader;
import dev.cominotti.java.evo.linter.core.LinterConfigOverrides;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

abstract class BaseCommand {
  @Spec private @Nullable CommandSpec spec;

  @Option(
      names = {"-c", "--config"},
      description = "Path to config TOML")
  @Nullable Path configPath;

  @Option(
      names = {"--project-root"},
      description = "Project root directory (default: current directory)")
  Path projectRoot = Path.of(".");

  @Option(
      names = {"--baseline"},
      description = "Override baseline file path")
  @Nullable Path baselinePath;

  @Option(
      names = {"--source-root"},
      description = "Override source root. Repeatable")
  List<Path> sourceRoots = new ArrayList<>();

  @Option(
      names = {"--classpath-entry"},
      description = "Additional classpath entry. Repeatable")
  List<Path> classpathEntries = new ArrayList<>();

  @Option(
      names = {"--disable-private"},
      description = "Disable private checks for fields and methods")
  boolean disablePrivate;

  @Option(
      names = {"--disable-package-private"},
      description = "Disable package-private checks for fields and methods")
  boolean disablePackagePrivate;

  @Option(
      names = {"--disable-private-fields"},
      description = "Disable private field checks")
  boolean disablePrivateFields;

  @Option(
      names = {"--disable-package-private-fields"},
      description = "Disable package-private field checks")
  boolean disablePackagePrivateFields;

  @Option(
      names = {"--disable-private-methods"},
      description = "Disable private method/constructor checks")
  boolean disablePrivateMethods;

  @Option(
      names = {"--disable-package-private-methods"},
      description = "Disable package-private method/constructor checks")
  boolean disablePackagePrivateMethods;

  protected Path normalizedProjectRoot() {
    return projectRoot.toAbsolutePath().normalize();
  }

  protected PrintWriter out() {
    return commandSpec().commandLine().getOut();
  }

  protected PrintWriter err() {
    return commandSpec().commandLine().getErr();
  }

  private CommandSpec commandSpec() {
    if (spec == null) {
      throw new IllegalStateException("Command spec is unavailable before picocli initialization");
    }
    return spec;
  }

  protected LinterConfig loadEffectiveConfig() throws IOException {
    var root = normalizedProjectRoot();
    var configLoader = new LinterConfigLoader();
    var config = configLoader.load(configPath, root);

    var overrides =
        new LinterConfigOverrides()
            .withBaselinePath(baselinePath)
            .withSourceRoots(sourceRoots)
            .withClasspathEntries(classpathEntries);

    if (disablePrivate || disablePrivateFields) {
      overrides = overrides.withIncludePrivateFields(Boolean.FALSE);
    }
    if (disablePackagePrivate || disablePackagePrivateFields) {
      overrides = overrides.withIncludePackagePrivateFields(Boolean.FALSE);
    }
    if (disablePrivate || disablePrivateMethods) {
      overrides = overrides.withIncludePrivateMethods(Boolean.FALSE);
    }
    if (disablePackagePrivate || disablePackagePrivateMethods) {
      overrides = overrides.withIncludePackagePrivateMethods(Boolean.FALSE);
    }

    return configLoader.applyOverrides(config, overrides, root);
  }
}
