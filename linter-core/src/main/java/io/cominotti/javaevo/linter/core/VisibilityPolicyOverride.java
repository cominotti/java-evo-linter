package io.cominotti.javaevo.linter.core;

import org.jspecify.annotations.Nullable;

public final class VisibilityPolicyOverride {
  public @Nullable Boolean includePrivate;
  public @Nullable Boolean includePackagePrivate;

  public void applyTo(VisibilityPolicy policy) {
    if (policy == null) {
      return;
    }
    if (includePrivate != null) {
      policy.includePrivate = includePrivate;
    }
    if (includePackagePrivate != null) {
      policy.includePackagePrivate = includePackagePrivate;
    }
  }
}
