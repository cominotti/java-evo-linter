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

    config.normalize();
    return config;
  }

  public LinterConfig applyOverrides(
      LinterConfig base, @Nullable LinterConfigOverrides overrides, Path projectRoot) {
    var effective = base.copy();

    if (overrides == null) {
      effective.normalize();
      return effective;
    }

    if (overrides.baselinePath != null) {
      effective.baseline.path = normalizePath(projectRoot, overrides.baselinePath).toString();
    }
    if (overrides.outputFormat != null) {
      effective.output.format = overrides.outputFormat;
    }
    if (overrides.jsonlPath != null) {
      effective.output.jsonlPath = normalizePath(projectRoot, overrides.jsonlPath).toString();
    }

    if (overrides.sourceRoots != null && !overrides.sourceRoots.isEmpty()) {
      effective.sourceRoots = new ArrayList<>();
      for (Path sourceRoot : overrides.sourceRoots) {
        effective.sourceRoots.add(normalizePath(projectRoot, sourceRoot).toString());
      }
    }

    if (overrides.classpathEntries != null && !overrides.classpathEntries.isEmpty()) {
      effective.classpath = new ArrayList<>();
      for (Path classpathEntry : overrides.classpathEntries) {
        effective.classpath.add(normalizePath(projectRoot, classpathEntry).toString());
      }
    }

    if (overrides.includePrivateFields != null) {
      effective.visibility.fields.includePrivate = overrides.includePrivateFields;
    }
    if (overrides.includePackagePrivateFields != null) {
      effective.visibility.fields.includePackagePrivate = overrides.includePackagePrivateFields;
    }
    if (overrides.includePrivateMethods != null) {
      effective.visibility.methods.includePrivate = overrides.includePrivateMethods;
    }
    if (overrides.includePackagePrivateMethods != null) {
      effective.visibility.methods.includePackagePrivate = overrides.includePackagePrivateMethods;
    }

    effective.normalize();
    return effective;
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

  private static boolean hasYamlExtension(Path path) {
    var fileNamePath = path.getFileName();
    if (fileNamePath == null) {
      return false;
    }
    var fileName = fileNamePath.toString().toLowerCase(Locale.ROOT);
    return fileName.endsWith(".yml") || fileName.endsWith(".yaml");
  }
}
