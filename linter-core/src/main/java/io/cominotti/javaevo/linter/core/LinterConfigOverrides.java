// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

public final class LinterConfigOverrides {
  public @Nullable Path baselinePath;
  public @Nullable OutputFormat outputFormat;
  public @Nullable Path jsonlPath;
  public List<Path> sourceRoots = new ArrayList<>();
  public List<Path> classpathEntries = new ArrayList<>();
  public @Nullable Boolean includePrivateFields;
  public @Nullable Boolean includePackagePrivateFields;
  public @Nullable Boolean includePrivateMethods;
  public @Nullable Boolean includePackagePrivateMethods;
}
