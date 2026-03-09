// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PrimitiveBoxedScannerTest {
  @TempDir Path tempDir;

  @Test
  void reportsForbiddenTypesInFieldsAndSignatures() throws Exception {
    writeSource(defaultSource());

    var config = new LinterConfig().withBaseline(new BaselineSettings(Boolean.FALSE, null));

    CheckReport report = new LinterEngine().check(tempDir, config);

    Assertions.assertThat(report.newFindings()).hasSize(5);
    Assertions.assertThat(report.activeFindings()).hasSize(5);
    Assertions.assertThat(report.rawFindingCount()).isEqualTo(6);
    Assertions.assertThat(report.inlineSuppressedCount()).isEqualTo(1);
    Assertions.assertThat(report.baselineSuppressedCount()).isZero();

    Assertions.assertThat(report.newFindings())
        .extracting(Finding::forbiddenType)
        .contains("int", "java.lang.Integer", "java.lang.String")
        .doesNotContain("java.lang.Long");

    Assertions.assertThat(report.newFindings())
        .extracting(Finding::violationRole)
        .contains("field_type", "method_return_type", "method_parameter_type");
  }

  @Test
  void appliesGlobalVisibilityDisablesAndPackageOverrides() throws Exception {
    writeSource(defaultSource());

    var config =
        new LinterConfig()
            .withBaseline(new BaselineSettings(Boolean.FALSE, null))
            .withVisibility(
                new VisibilitySettings(
                    new VisibilityPolicy(Boolean.FALSE, Boolean.TRUE),
                    new VisibilityPolicy(Boolean.FALSE, Boolean.TRUE)));

    CheckReport withoutOverride = new LinterEngine().check(tempDir, config);
    Assertions.assertThat(withoutOverride.newFindings()).hasSize(4);
    Assertions.assertThat(withoutOverride.rawFindingCount()).isEqualTo(4);

    var packageOverride =
        new PackageVisibilityOverride(
            "com.acme.**",
            new VisibilityPolicyOverride(Boolean.TRUE, null),
            new VisibilityPolicyOverride(Boolean.TRUE, null));

    config = config.withPackageOverrides(List.of(packageOverride));

    CheckReport withOverride = new LinterEngine().check(tempDir, config);
    Assertions.assertThat(withOverride.newFindings()).hasSize(5);
    Assertions.assertThat(withOverride.rawFindingCount()).isEqualTo(6);
    Assertions.assertThat(withOverride.inlineSuppressedCount()).isEqualTo(1);
  }

  private void writeSource(String source) throws IOException {
    Path file = tempDir.resolve("src/main/java/com/acme/Example.java");
    Files.createDirectories(file.getParent());
    Files.writeString(file, source);
  }

  private String defaultSource() {
    return """
                package com.acme;

                import java.util.List;

                public class Example {
                    private int primitiveField;
                    Integer boxedField;
                    private Value valueField;

                    public String name() {
                        return "x";
                    }

                    void setValues(Integer amount, List<String> names, Value value) {
                    }

                    @SuppressWarnings("primitive-boxed-signature")
                    private void suppressed(Long id) {
                    }

                    static class Value {
                    }
                }
                """;
  }
}
