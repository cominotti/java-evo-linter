// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

public final class VisibilityPolicy {
  public boolean includePrivate = true;
  public boolean includePackagePrivate = true;

  public VisibilityPolicy copy() {
    var copy = new VisibilityPolicy();
    copy.includePrivate = includePrivate;
    copy.includePackagePrivate = includePackagePrivate;
    return copy;
  }

  public boolean shouldCheck(Visibility visibility) {
    return switch (visibility) {
      case PUBLIC, PROTECTED -> true;
      case PRIVATE -> includePrivate;
      case PACKAGE_PRIVATE -> includePackagePrivate;
    };
  }
}
