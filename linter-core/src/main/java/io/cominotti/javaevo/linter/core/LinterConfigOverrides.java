// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record LinterConfigOverrides(
    @Nullable Path baselinePath,
    @Nullable OutputFormat outputFormat,
    @Nullable Path jsonlPath,
    List<Path> sourceRoots,
    List<Path> classpathEntries,
    @Nullable Boolean includePrivateFields,
    @Nullable Boolean includePackagePrivateFields,
    @Nullable Boolean includePrivateMethods,
    @Nullable Boolean includePackagePrivateMethods) {
  public LinterConfigOverrides {
    sourceRoots = sourceRoots == null ? List.of() : List.copyOf(sourceRoots);
    classpathEntries = classpathEntries == null ? List.of() : List.copyOf(classpathEntries);
  }

  public LinterConfigOverrides() {
    this(null, null, null, List.of(), List.of(), null, null, null, null);
  }

  public LinterConfigOverrides withBaselinePath(@Nullable Path value) {
    return new LinterConfigOverrides(
        value,
        outputFormat,
        jsonlPath,
        sourceRoots,
        classpathEntries,
        includePrivateFields,
        includePackagePrivateFields,
        includePrivateMethods,
        includePackagePrivateMethods);
  }

  public LinterConfigOverrides withOutputFormat(@Nullable OutputFormat value) {
    return new LinterConfigOverrides(
        baselinePath,
        value,
        jsonlPath,
        sourceRoots,
        classpathEntries,
        includePrivateFields,
        includePackagePrivateFields,
        includePrivateMethods,
        includePackagePrivateMethods);
  }

  public LinterConfigOverrides withJsonlPath(@Nullable Path value) {
    return new LinterConfigOverrides(
        baselinePath,
        outputFormat,
        value,
        sourceRoots,
        classpathEntries,
        includePrivateFields,
        includePackagePrivateFields,
        includePrivateMethods,
        includePackagePrivateMethods);
  }

  public LinterConfigOverrides withSourceRoots(List<Path> value) {
    return new LinterConfigOverrides(
        baselinePath,
        outputFormat,
        jsonlPath,
        value,
        classpathEntries,
        includePrivateFields,
        includePackagePrivateFields,
        includePrivateMethods,
        includePackagePrivateMethods);
  }

  public LinterConfigOverrides withClasspathEntries(List<Path> value) {
    return new LinterConfigOverrides(
        baselinePath,
        outputFormat,
        jsonlPath,
        sourceRoots,
        value,
        includePrivateFields,
        includePackagePrivateFields,
        includePrivateMethods,
        includePackagePrivateMethods);
  }

  public LinterConfigOverrides withIncludePrivateFields(@Nullable Boolean value) {
    return new LinterConfigOverrides(
        baselinePath,
        outputFormat,
        jsonlPath,
        sourceRoots,
        classpathEntries,
        value,
        includePackagePrivateFields,
        includePrivateMethods,
        includePackagePrivateMethods);
  }

  public LinterConfigOverrides withIncludePackagePrivateFields(@Nullable Boolean value) {
    return new LinterConfigOverrides(
        baselinePath,
        outputFormat,
        jsonlPath,
        sourceRoots,
        classpathEntries,
        includePrivateFields,
        value,
        includePrivateMethods,
        includePackagePrivateMethods);
  }

  public LinterConfigOverrides withIncludePrivateMethods(@Nullable Boolean value) {
    return new LinterConfigOverrides(
        baselinePath,
        outputFormat,
        jsonlPath,
        sourceRoots,
        classpathEntries,
        includePrivateFields,
        includePackagePrivateFields,
        value,
        includePackagePrivateMethods);
  }

  public LinterConfigOverrides withIncludePackagePrivateMethods(@Nullable Boolean value) {
    return new LinterConfigOverrides(
        baselinePath,
        outputFormat,
        jsonlPath,
        sourceRoots,
        classpathEntries,
        includePrivateFields,
        includePackagePrivateFields,
        includePrivateMethods,
        value);
  }
}
