package io.cominotti.javaevo.linter.core;

import java.util.List;

public final class VisibilityResolver {
  private final VisibilitySettings visibilitySettings;
  private final List<PackageVisibilityOverride> packageOverrides;

  public VisibilityResolver(
      VisibilitySettings visibilitySettings, List<PackageVisibilityOverride> packageOverrides) {
    this.visibilitySettings = visibilitySettings;
    this.packageOverrides = packageOverrides;
  }

  public boolean shouldCheckField(String packageName, Visibility visibility) {
    var effective = visibilitySettings.fields.copy();
    applyOverrides(packageName, effective, true);
    return effective.shouldCheck(visibility);
  }

  public boolean shouldCheckMethod(String packageName, Visibility visibility) {
    var effective = visibilitySettings.methods.copy();
    applyOverrides(packageName, effective, false);
    return effective.shouldCheck(visibility);
  }

  private void applyOverrides(String packageName, VisibilityPolicy effective, boolean fieldPolicy) {
    if (packageOverrides == null || packageOverrides.isEmpty()) {
      return;
    }

    for (PackageVisibilityOverride packageOverride : packageOverrides) {
      if (packageOverride == null
          || packageOverride.pattern == null
          || packageOverride.pattern.isBlank()) {
        continue;
      }

      if (!PackagePatternMatcher.matches(packageOverride.pattern, packageName)) {
        continue;
      }

      VisibilityPolicyOverride overridePolicy =
          fieldPolicy ? packageOverride.fields : packageOverride.methods;
      if (overridePolicy != null) {
        overridePolicy.applyTo(effective);
      }
    }
  }
}
