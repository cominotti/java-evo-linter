// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

import org.jspecify.annotations.Nullable;

/** Pattern-based visibility override for a package tree. */
public record PackageVisibilityOverride(
    @Nullable String pattern,
    @Nullable VisibilityPolicyOverride fields,
    @Nullable VisibilityPolicyOverride methods) {
  public PackageVisibilityOverride() {
    this(null, null, null);
  }
}
