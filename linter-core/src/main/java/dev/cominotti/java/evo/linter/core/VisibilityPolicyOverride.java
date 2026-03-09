// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

import org.jspecify.annotations.Nullable;

/** Package-scoped visibility override applied on top of a base visibility policy. */
public record VisibilityPolicyOverride(
    @Nullable Boolean includePrivate, @Nullable Boolean includePackagePrivate) {
  public VisibilityPolicyOverride() {
    this(null, null);
  }

  public VisibilityPolicy applyTo(VisibilityPolicy policy) {
    if (policy == null) {
      return new VisibilityPolicy();
    }
    var effectivePrivate = includePrivate != null ? includePrivate : policy.includePrivate();
    var effectivePackagePrivate =
        includePackagePrivate != null ? includePackagePrivate : policy.includePackagePrivate();
    return new VisibilityPolicy(effectivePrivate, effectivePackagePrivate);
  }
}
