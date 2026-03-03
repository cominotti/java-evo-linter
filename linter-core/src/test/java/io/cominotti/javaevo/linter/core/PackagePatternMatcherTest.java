package io.cominotti.javaevo.linter.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PackagePatternMatcherTest {
  @Test
  void matchesAntStylePackagePatterns() {
    assertThat(PackagePatternMatcher.matches("com.acme.**", "com.acme")).isTrue();
    assertThat(PackagePatternMatcher.matches("com.acme.**", "com.acme.orders.internal")).isTrue();
    assertThat(PackagePatternMatcher.matches("com.acme.*", "com.acme.orders")).isTrue();
    assertThat(PackagePatternMatcher.matches("com.acme.*", "com.acme.orders.internal")).isFalse();
    assertThat(PackagePatternMatcher.matches("org.example", "org.example")).isTrue();
    assertThat(PackagePatternMatcher.matches("org.example", "org.example.sub")).isFalse();
  }
}
