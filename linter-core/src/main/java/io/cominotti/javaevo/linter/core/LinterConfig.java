// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record LinterConfig(
    List<String> sourceRoots,
    List<String> includeGlobs,
    List<String> excludeGlobs,
    VisibilitySettings visibility,
    List<PackageVisibilityOverride> packageOverrides,
    List<String> forbiddenTypes,
    AnnotatedTypeExclusions annotatedTypeExclusions,
    SuppressionSettings suppression,
    BaselineSettings baseline,
    OutputSettings output,
    List<String> classpath,
    Boolean failOnCompileErrors) {
  private static final List<String> DEFAULT_SOURCE_ROOTS = List.of("src/main/java");
  private static final List<String> DEFAULT_INCLUDE_GLOBS = List.of("**/*.java");

  public LinterConfig {
    sourceRoots = normalizeListOrDefault(sourceRoots, DEFAULT_SOURCE_ROOTS);
    includeGlobs = normalizeListOrDefault(includeGlobs, DEFAULT_INCLUDE_GLOBS);
    excludeGlobs = normalizeListOrEmpty(excludeGlobs);
    visibility = visibility == null ? new VisibilitySettings() : visibility;
    packageOverrides = packageOverrides == null ? List.of() : List.copyOf(packageOverrides);
    forbiddenTypes = normalizeForbiddenTypes(forbiddenTypes);
    annotatedTypeExclusions =
        annotatedTypeExclusions == null ? new AnnotatedTypeExclusions() : annotatedTypeExclusions;
    suppression = suppression == null ? new SuppressionSettings() : suppression;
    baseline = baseline == null ? new BaselineSettings() : baseline;
    output = output == null ? new OutputSettings() : output;
    classpath = normalizeListOrEmpty(classpath);
    failOnCompileErrors = failOnCompileErrors == null ? Boolean.TRUE : failOnCompileErrors;
  }

  public LinterConfig() {
    this(
        DEFAULT_SOURCE_ROOTS,
        DEFAULT_INCLUDE_GLOBS,
        List.of(),
        new VisibilitySettings(),
        List.of(),
        ForbiddenTypeCatalog.defaultForbiddenTypes(),
        new AnnotatedTypeExclusions(),
        new SuppressionSettings(),
        new BaselineSettings(),
        new OutputSettings(),
        List.of(),
        Boolean.TRUE);
  }

  public LinterConfig withSourceRoots(List<String> value) {
    return new LinterConfig(
        value,
        includeGlobs,
        excludeGlobs,
        visibility,
        packageOverrides,
        forbiddenTypes,
        annotatedTypeExclusions,
        suppression,
        baseline,
        output,
        classpath,
        failOnCompileErrors);
  }

  public LinterConfig withIncludeGlobs(List<String> value) {
    return new LinterConfig(
        sourceRoots,
        value,
        excludeGlobs,
        visibility,
        packageOverrides,
        forbiddenTypes,
        annotatedTypeExclusions,
        suppression,
        baseline,
        output,
        classpath,
        failOnCompileErrors);
  }

  public LinterConfig withExcludeGlobs(List<String> value) {
    return new LinterConfig(
        sourceRoots,
        includeGlobs,
        value,
        visibility,
        packageOverrides,
        forbiddenTypes,
        annotatedTypeExclusions,
        suppression,
        baseline,
        output,
        classpath,
        failOnCompileErrors);
  }

  public LinterConfig withVisibility(VisibilitySettings value) {
    return new LinterConfig(
        sourceRoots,
        includeGlobs,
        excludeGlobs,
        value,
        packageOverrides,
        forbiddenTypes,
        annotatedTypeExclusions,
        suppression,
        baseline,
        output,
        classpath,
        failOnCompileErrors);
  }

  public LinterConfig withPackageOverrides(List<PackageVisibilityOverride> value) {
    return new LinterConfig(
        sourceRoots,
        includeGlobs,
        excludeGlobs,
        visibility,
        value,
        forbiddenTypes,
        annotatedTypeExclusions,
        suppression,
        baseline,
        output,
        classpath,
        failOnCompileErrors);
  }

  public LinterConfig withForbiddenTypes(List<String> value) {
    return new LinterConfig(
        sourceRoots,
        includeGlobs,
        excludeGlobs,
        visibility,
        packageOverrides,
        value,
        annotatedTypeExclusions,
        suppression,
        baseline,
        output,
        classpath,
        failOnCompileErrors);
  }

  public LinterConfig withAnnotatedTypeExclusions(AnnotatedTypeExclusions value) {
    return new LinterConfig(
        sourceRoots,
        includeGlobs,
        excludeGlobs,
        visibility,
        packageOverrides,
        forbiddenTypes,
        value,
        suppression,
        baseline,
        output,
        classpath,
        failOnCompileErrors);
  }

  public LinterConfig withSuppression(SuppressionSettings value) {
    return new LinterConfig(
        sourceRoots,
        includeGlobs,
        excludeGlobs,
        visibility,
        packageOverrides,
        forbiddenTypes,
        annotatedTypeExclusions,
        value,
        baseline,
        output,
        classpath,
        failOnCompileErrors);
  }

  public LinterConfig withBaseline(BaselineSettings value) {
    return new LinterConfig(
        sourceRoots,
        includeGlobs,
        excludeGlobs,
        visibility,
        packageOverrides,
        forbiddenTypes,
        annotatedTypeExclusions,
        suppression,
        value,
        output,
        classpath,
        failOnCompileErrors);
  }

  public LinterConfig withBaselinePath(String value) {
    return withBaseline(baseline.withPath(value));
  }

  public LinterConfig withOutput(OutputSettings value) {
    return new LinterConfig(
        sourceRoots,
        includeGlobs,
        excludeGlobs,
        visibility,
        packageOverrides,
        forbiddenTypes,
        annotatedTypeExclusions,
        suppression,
        baseline,
        value,
        classpath,
        failOnCompileErrors);
  }

  public LinterConfig withOutputFormat(OutputFormat value) {
    return withOutput(output.withFormat(value));
  }

  public LinterConfig withOutputJsonlPath(String value) {
    return withOutput(output.withJsonlPath(value));
  }

  public LinterConfig withClasspath(List<String> value) {
    return new LinterConfig(
        sourceRoots,
        includeGlobs,
        excludeGlobs,
        visibility,
        packageOverrides,
        forbiddenTypes,
        annotatedTypeExclusions,
        suppression,
        baseline,
        output,
        value,
        failOnCompileErrors);
  }

  public LinterConfig withFailOnCompileErrors(boolean value) {
    return new LinterConfig(
        sourceRoots,
        includeGlobs,
        excludeGlobs,
        visibility,
        packageOverrides,
        forbiddenTypes,
        annotatedTypeExclusions,
        suppression,
        baseline,
        output,
        classpath,
        value);
  }

  private static List<String> normalizeListOrDefault(
      @Nullable List<String> values, List<String> defaults) {
    if (values == null || values.isEmpty()) {
      return List.copyOf(defaults);
    }
    return List.copyOf(values);
  }

  private static List<String> normalizeListOrEmpty(@Nullable List<String> values) {
    if (values == null) {
      return List.of();
    }
    return List.copyOf(values);
  }

  private static List<String> normalizeForbiddenTypes(@Nullable List<String> values) {
    if (values == null || values.isEmpty()) {
      return List.copyOf(ForbiddenTypeCatalog.defaultForbiddenTypes());
    }
    var normalized = new LinkedHashSet<String>();
    for (String forbiddenType : values) {
      normalized.add(ForbiddenTypeCatalog.normalizeConfiguredType(forbiddenType));
    }
    return List.copyOf(new ArrayList<>(normalized));
  }
}
