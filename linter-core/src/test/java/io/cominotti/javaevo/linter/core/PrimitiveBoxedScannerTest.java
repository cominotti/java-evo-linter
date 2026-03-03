package io.cominotti.javaevo.linter.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PrimitiveBoxedScannerTest {
  @TempDir Path tempDir;

  @Test
  void reportsForbiddenTypesInFieldsAndSignatures() throws Exception {
    writeSource(defaultSource());

    LinterConfig config = new LinterConfig();
    config.baseline.enabled = false;

    CheckReport report = new LinterEngine().check(tempDir, config);

    assertThat(report.newFindings()).hasSize(5);
    assertThat(report.activeFindings()).hasSize(5);
    assertThat(report.rawFindingCount()).isEqualTo(6);
    assertThat(report.inlineSuppressedCount()).isEqualTo(1);
    assertThat(report.baselineSuppressedCount()).isZero();

    assertThat(report.newFindings())
        .extracting(Finding::forbiddenType)
        .contains("int", "java.lang.Integer", "java.lang.String")
        .doesNotContain("java.lang.Long");

    assertThat(report.newFindings())
        .extracting(Finding::violationRole)
        .contains("field_type", "method_return_type", "method_parameter_type");
  }

  @Test
  void appliesGlobalVisibilityDisablesAndPackageOverrides() throws Exception {
    writeSource(defaultSource());

    LinterConfig config = new LinterConfig();
    config.baseline.enabled = false;
    config.visibility.fields.includePrivate = false;
    config.visibility.methods.includePrivate = false;

    CheckReport withoutOverride = new LinterEngine().check(tempDir, config);
    assertThat(withoutOverride.newFindings()).hasSize(4);
    assertThat(withoutOverride.rawFindingCount()).isEqualTo(4);

    PackageVisibilityOverride packageOverride = new PackageVisibilityOverride();
    packageOverride.pattern = "com.acme.**";

    VisibilityPolicyOverride fields = new VisibilityPolicyOverride();
    fields.includePrivate = true;
    packageOverride.fields = fields;

    VisibilityPolicyOverride methods = new VisibilityPolicyOverride();
    methods.includePrivate = true;
    packageOverride.methods = methods;

    config.packageOverrides = List.of(packageOverride);

    CheckReport withOverride = new LinterEngine().check(tempDir, config);
    assertThat(withOverride.newFindings()).hasSize(5);
    assertThat(withOverride.rawFindingCount()).isEqualTo(6);
    assertThat(withOverride.inlineSuppressedCount()).isEqualTo(1);
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
