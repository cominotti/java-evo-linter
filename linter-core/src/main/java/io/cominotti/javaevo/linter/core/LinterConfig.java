// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class LinterConfig {
  public List<String> sourceRoots = new ArrayList<>(List.of("src/main/java"));
  public List<String> includeGlobs = new ArrayList<>(List.of("**/*.java"));
  public List<String> excludeGlobs = new ArrayList<>();
  public VisibilitySettings visibility = new VisibilitySettings();
  public List<PackageVisibilityOverride> packageOverrides = new ArrayList<>();
  public List<String> forbiddenTypes =
      new ArrayList<>(ForbiddenTypeCatalog.defaultForbiddenTypes());
  public AnnotatedTypeExclusions annotatedTypeExclusions = new AnnotatedTypeExclusions();
  public SuppressionSettings suppression = new SuppressionSettings();
  public BaselineSettings baseline = new BaselineSettings();
  public OutputSettings output = new OutputSettings();
  public List<String> classpath = new ArrayList<>();
  public boolean failOnCompileErrors = true;

  public void normalize() {
    if (sourceRoots == null || sourceRoots.isEmpty()) {
      sourceRoots = new ArrayList<>(List.of("src/main/java"));
    }
    if (includeGlobs == null || includeGlobs.isEmpty()) {
      includeGlobs = new ArrayList<>(List.of("**/*.java"));
    }
    if (excludeGlobs == null) {
      excludeGlobs = new ArrayList<>();
    }
    if (visibility == null) {
      visibility = new VisibilitySettings();
    }
    if (visibility.fields == null) {
      visibility.fields = new VisibilityPolicy();
    }
    if (visibility.methods == null) {
      visibility.methods = new VisibilityPolicy();
    }
    if (packageOverrides == null) {
      packageOverrides = new ArrayList<>();
    }
    if (forbiddenTypes == null || forbiddenTypes.isEmpty()) {
      forbiddenTypes = new ArrayList<>(ForbiddenTypeCatalog.defaultForbiddenTypes());
    } else {
      var normalized = new LinkedHashSet<String>();
      for (String forbiddenType : forbiddenTypes) {
        normalized.add(ForbiddenTypeCatalog.normalizeConfiguredType(forbiddenType));
      }
      forbiddenTypes = new ArrayList<>(normalized);
    }
    if (annotatedTypeExclusions == null) {
      annotatedTypeExclusions = new AnnotatedTypeExclusions();
    }
    annotatedTypeExclusions.normalize();
    if (suppression == null) {
      suppression = new SuppressionSettings();
    }
    if (suppression.keys == null || suppression.keys.isEmpty()) {
      suppression.keys =
          new ArrayList<>(
              List.of(
                  RuleIds.PRIMITIVE_BOXED_SIGNATURE,
                  "java-evo-linter:" + RuleIds.PRIMITIVE_BOXED_SIGNATURE));
    }
    if (baseline == null) {
      baseline = new BaselineSettings();
    }
    if (baseline.path == null || baseline.path.isBlank()) {
      baseline.path = ".java-evo-linter-baseline.jsonl";
    }
    if (output == null) {
      output = new OutputSettings();
    }
    if (output.format == null) {
      output.format = OutputFormat.BOTH;
    }
    if (output.jsonlPath == null || output.jsonlPath.isBlank()) {
      output.jsonlPath = ".java-evo-linter-findings.jsonl";
    }
    if (classpath == null) {
      classpath = new ArrayList<>();
    }
  }

  public LinterConfig copy() {
    var copy = new LinterConfig();
    copy.sourceRoots = new ArrayList<>(sourceRoots);
    copy.includeGlobs = new ArrayList<>(includeGlobs);
    copy.excludeGlobs = new ArrayList<>(excludeGlobs);
    copy.visibility = visibility.copy();
    copy.packageOverrides = new ArrayList<>(packageOverrides);
    copy.forbiddenTypes = new ArrayList<>(forbiddenTypes);
    copy.annotatedTypeExclusions =
        annotatedTypeExclusions == null
            ? new AnnotatedTypeExclusions()
            : annotatedTypeExclusions.copy();
    copy.suppression = suppression.copy();
    copy.baseline = baseline.copy();
    copy.output = output.copy();
    copy.classpath = new ArrayList<>(classpath);
    copy.failOnCompileErrors = failOnCompileErrors;
    return copy;
  }
}
