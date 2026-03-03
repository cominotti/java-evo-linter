// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

public record VisibilityPolicy(Boolean includePrivate, Boolean includePackagePrivate) {
  public VisibilityPolicy {
    includePrivate = includePrivate == null ? Boolean.TRUE : includePrivate;
    includePackagePrivate = includePackagePrivate == null ? Boolean.TRUE : includePackagePrivate;
  }

  public VisibilityPolicy() {
    this(Boolean.TRUE, Boolean.TRUE);
  }

  public VisibilityPolicy withIncludePrivate(boolean value) {
    return new VisibilityPolicy(value, includePackagePrivate);
  }

  public VisibilityPolicy withIncludePackagePrivate(boolean value) {
    return new VisibilityPolicy(includePrivate, value);
  }

  public boolean shouldCheck(Visibility visibility) {
    return switch (visibility) {
      case PUBLIC, PROTECTED -> true;
      case PRIVATE -> includePrivate;
      case PACKAGE_PRIVATE -> includePackagePrivate;
    };
  }
}
