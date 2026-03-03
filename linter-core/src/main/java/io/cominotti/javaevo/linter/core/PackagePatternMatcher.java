package io.cominotti.javaevo.linter.core;

public final class PackagePatternMatcher {
  private PackagePatternMatcher() {}

  public static boolean matches(String pattern, String packageName) {
    if (pattern == null || pattern.isBlank()) {
      return false;
    }

    var patternSegments = pattern.split("\\.");
    String[] packageSegments =
        packageName == null || packageName.isBlank() ? new String[0] : packageName.split("\\.");

    return matchSegments(patternSegments, 0, packageSegments, 0);
  }

  private static boolean matchSegments(
      String[] patternSegments, int patternIndex, String[] packageSegments, int packageIndex) {
    if (patternIndex == patternSegments.length) {
      return packageIndex == packageSegments.length;
    }

    var patternSegment = patternSegments[patternIndex];
    if ("**".equals(patternSegment)) {
      for (var current = packageIndex; current <= packageSegments.length; current++) {
        if (matchSegments(patternSegments, patternIndex + 1, packageSegments, current)) {
          return true;
        }
      }
      return false;
    }

    if (packageIndex >= packageSegments.length) {
      return false;
    }

    if (!matchSingleSegment(patternSegment, packageSegments[packageIndex])) {
      return false;
    }

    return matchSegments(patternSegments, patternIndex + 1, packageSegments, packageIndex + 1);
  }

  private static boolean matchSingleSegment(String patternSegment, String valueSegment) {
    if (!patternSegment.contains("*")) {
      return patternSegment.equals(valueSegment);
    }

    var regex = new StringBuilder();
    for (var index = 0; index < patternSegment.length(); index++) {
      var current = patternSegment.charAt(index);
      if (current == '*') {
        regex.append(".*");
      } else {
        if ("\\.^$+?()[]{}|".indexOf(current) >= 0) {
          regex.append('\\');
        }
        regex.append(current);
      }
    }

    return valueSegment.matches(regex.toString());
  }
}
