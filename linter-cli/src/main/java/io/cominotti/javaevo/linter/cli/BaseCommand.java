package io.cominotti.javaevo.linter.cli;

import io.cominotti.javaevo.linter.core.LinterConfig;
import io.cominotti.javaevo.linter.core.LinterConfigLoader;
import io.cominotti.javaevo.linter.core.LinterConfigOverrides;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Option;

abstract class BaseCommand {
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

  protected LinterConfig loadEffectiveConfig() throws Exception {
    var root = normalizedProjectRoot();
    var configLoader = new LinterConfigLoader();
    var config = configLoader.load(configPath, root);

    var overrides = new LinterConfigOverrides();
    overrides.baselinePath = baselinePath;
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

    return configLoader.applyOverrides(config, overrides, root);
  }
}
