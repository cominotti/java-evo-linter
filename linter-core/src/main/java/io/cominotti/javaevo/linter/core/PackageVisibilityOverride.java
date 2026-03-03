// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import org.jspecify.annotations.Nullable;

public final class PackageVisibilityOverride {
  public @Nullable String pattern;
  public @Nullable VisibilityPolicyOverride fields;
  public @Nullable VisibilityPolicyOverride methods;
}
