// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import org.jspecify.annotations.Nullable;

public record PackageVisibilityOverride(
    @Nullable String pattern,
    @Nullable VisibilityPolicyOverride fields,
    @Nullable VisibilityPolicyOverride methods) {
  public PackageVisibilityOverride() {
    this(null, null, null);
  }
}
