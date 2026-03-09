// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

import java.util.List;

final class VisibilityResolver {
  private final VisibilitySettings visibilitySettings;
  private final List<PackageVisibilityOverride> packageOverrides;

  public VisibilityResolver(
      VisibilitySettings visibilitySettings, List<PackageVisibilityOverride> packageOverrides) {
    this.visibilitySettings = visibilitySettings;
    this.packageOverrides = packageOverrides;
  }

  public boolean shouldCheckField(String packageName, Visibility visibility) {
    var effective = applyOverrides(packageName, visibilitySettings.fields(), true);
    return effective.shouldCheck(visibility);
  }

  public boolean shouldCheckMethod(String packageName, Visibility visibility) {
    var effective = applyOverrides(packageName, visibilitySettings.methods(), false);
    return effective.shouldCheck(visibility);
  }

  private VisibilityPolicy applyOverrides(
      String packageName, VisibilityPolicy basePolicy, boolean fieldPolicy) {
    if (packageOverrides == null || packageOverrides.isEmpty()) {
      return basePolicy;
    }

    var effective = basePolicy;
    for (PackageVisibilityOverride packageOverride : packageOverrides) {
      if (matchesPackageOverride(packageOverride, packageName)) {
        VisibilityPolicyOverride overridePolicy =
            fieldPolicy ? packageOverride.fields() : packageOverride.methods();
        if (overridePolicy != null) {
          effective = overridePolicy.applyTo(effective);
        }
      }
    }
    return effective;
  }

  private boolean matchesPackageOverride(
      PackageVisibilityOverride packageOverride, String packageName) {
    if (packageOverride == null) {
      return false;
    }
    var pattern = packageOverride.pattern();
    return pattern != null
        && !pattern.isBlank()
        && PackagePatternMatcher.matches(pattern, packageName);
  }
}
