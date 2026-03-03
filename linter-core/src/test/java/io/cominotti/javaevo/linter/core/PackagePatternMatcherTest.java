// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class PackagePatternMatcherTest {
  @Test
  void matchesAntStylePackagePatterns() {
    Assertions.assertThat(PackagePatternMatcher.matches("com.acme.**", "com.acme")).isTrue();
    Assertions.assertThat(PackagePatternMatcher.matches("com.acme.**", "com.acme.orders.internal"))
        .isTrue();
    Assertions.assertThat(PackagePatternMatcher.matches("com.acme.*", "com.acme.orders")).isTrue();
    Assertions.assertThat(PackagePatternMatcher.matches("com.acme.*", "com.acme.orders.internal"))
        .isFalse();
    Assertions.assertThat(PackagePatternMatcher.matches("org.example", "org.example")).isTrue();
    Assertions.assertThat(PackagePatternMatcher.matches("org.example", "org.example.sub"))
        .isFalse();
  }
}
