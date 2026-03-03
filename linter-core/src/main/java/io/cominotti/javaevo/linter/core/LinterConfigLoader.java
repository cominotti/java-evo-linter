// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

public final class LinterConfigLoader {
  private static final Path DEFAULT_CONFIG_PATH = Path.of(".java-evo-linter.toml");

  private final ObjectMapper objectMapper;

  public LinterConfigLoader() {
    this.objectMapper =
        new ObjectMapper(new TomlFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
  }

  public LinterConfig load(@Nullable Path explicitConfigPath, Path projectRoot) throws IOException {
    var configPath = explicitConfigPath;
    if (configPath == null) {
      var candidate = projectRoot.resolve(DEFAULT_CONFIG_PATH);
      if (Files.exists(candidate)) {
        configPath = candidate;
      }
    }

    LinterConfig config;
    if (configPath == null) {
      config = new LinterConfig();
    } else {
      Path normalizedPath = normalizePath(projectRoot, configPath);
      if (hasYamlExtension(normalizedPath)) {
        throw new IOException(
            "YAML config is no longer supported: "
                + normalizedPath
                + ". Use TOML (.toml) instead.");
      }
      if (!Files.exists(normalizedPath)) {
        throw new IOException("Config file not found: " + normalizedPath);
      }
      config = objectMapper.readValue(normalizedPath.toFile(), LinterConfig.class);
    }
    return config;
  }

  public LinterConfig applyOverrides(
      LinterConfig base, @Nullable LinterConfigOverrides overrides, Path projectRoot) {
    if (overrides == null) {
      return base;
    }

    var effective = applyOutputAndBaselineOverrides(base, overrides, projectRoot);
    effective = applyPathListOverrides(effective, overrides, projectRoot);
    return applyVisibilityOverrides(effective, overrides);
  }

  public static Path normalizePath(Path projectRoot, Path value) {
    if (value.isAbsolute()) {
      return value.normalize();
    }
    return projectRoot.resolve(value).normalize();
  }

  public static List<Path> normalizePaths(Path projectRoot, List<String> paths) {
    var normalized = new ArrayList<Path>();
    for (String path : paths) {
      normalized.add(normalizePath(projectRoot, Path.of(path)));
    }
    return normalized;
  }

  private static LinterConfig applyOutputAndBaselineOverrides(
      LinterConfig base, LinterConfigOverrides overrides, Path projectRoot) {
    var effective = base;
    if (overrides.baselinePath() != null) {
      effective =
          effective.withBaselinePath(
              normalizePath(projectRoot, overrides.baselinePath()).toString());
    }
    if (overrides.outputFormat() != null) {
      effective = effective.withOutputFormat(overrides.outputFormat());
    }
    if (overrides.jsonlPath() != null) {
      effective =
          effective.withOutputJsonlPath(
              normalizePath(projectRoot, overrides.jsonlPath()).toString());
    }
    return effective;
  }

  private static LinterConfig applyPathListOverrides(
      LinterConfig base, LinterConfigOverrides overrides, Path projectRoot) {
    var effective = base;
    if (!overrides.sourceRoots().isEmpty()) {
      effective =
          effective.withSourceRoots(normalizePathStrings(projectRoot, overrides.sourceRoots()));
    }
    if (!overrides.classpathEntries().isEmpty()) {
      effective =
          effective.withClasspath(normalizePathStrings(projectRoot, overrides.classpathEntries()));
    }
    return effective;
  }

  private static LinterConfig applyVisibilityOverrides(
      LinterConfig base, LinterConfigOverrides overrides) {
    var fields = base.visibility().fields();
    if (overrides.includePrivateFields() != null) {
      fields = fields.withIncludePrivate(overrides.includePrivateFields());
    }
    if (overrides.includePackagePrivateFields() != null) {
      fields = fields.withIncludePackagePrivate(overrides.includePackagePrivateFields());
    }

    var methods = base.visibility().methods();
    if (overrides.includePrivateMethods() != null) {
      methods = methods.withIncludePrivate(overrides.includePrivateMethods());
    }
    if (overrides.includePackagePrivateMethods() != null) {
      methods = methods.withIncludePackagePrivate(overrides.includePackagePrivateMethods());
    }
    return base.withVisibility(new VisibilitySettings(fields, methods));
  }

  private static List<String> normalizePathStrings(Path projectRoot, List<Path> paths) {
    var normalized = new ArrayList<String>();
    for (Path path : paths) {
      normalized.add(normalizePath(projectRoot, path).toString());
    }
    return List.copyOf(normalized);
  }

  private static boolean hasYamlExtension(Path path) {
    var fileNamePath = path.getFileName();
    if (fileNamePath == null) {
      return false;
    }
    var fileName = fileNamePath.toString().toLowerCase(Locale.ROOT);
    return fileName.endsWith(".yml") || fileName.endsWith(".yaml");
  }
}
