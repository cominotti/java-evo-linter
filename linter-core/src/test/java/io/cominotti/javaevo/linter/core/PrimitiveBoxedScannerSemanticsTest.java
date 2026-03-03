// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PrimitiveBoxedScannerSemanticsTest {
  @TempDir Path tempDir;

  @Test
  void detectsNestedSignatureShapesAndRecordComponents() throws Exception {
    writeSource(
        "src/main/java/com/acme/Semantics.java",
        """
                package com.acme;

                import java.util.List;
                import java.util.Map;

                record Payload(String id, List<Integer> ids) {
                }

                class Semantics {
                    int[] values;
                    List<? extends Integer> boxed;

                    public Semantics(Integer... incoming) {
                    }

                    public Map<String, List<Integer[]>> work(Integer[] in, Payload payload) {
                        int localPrimitive = 1;
                        Integer localBoxed = 2;
                        String localString = "s";
                        return null;
                    }
                }
                """);

    CheckReport report = runCheck(defaultConfig());

    assertThat(report.newFindings()).hasSize(10);
    assertThat(report.rawFindingCount()).isEqualTo(10);
    assertThat(report.inlineSuppressedCount()).isZero();

    assertThat(report.newFindings())
        .extracting(Finding::forbiddenType)
        .contains("int", "java.lang.Integer", "java.lang.String");

    assertThat(report.newFindings())
        .extracting(Finding::violationRole)
        .contains("field_type", "method_parameter_type", "method_return_type");

    assertThat(report.newFindings())
        .extracting(Finding::memberKind)
        .contains("constructor", "field", "method");

    assertThat(report.newFindings())
        .extracting(Finding::memberSignature)
        .noneMatch(signature -> signature.contains("local"));
  }

  @Test
  void classLevelSuppressWarningsAllSuppressesEverythingInScope() throws Exception {
    writeSource(
        "src/main/java/com/acme/Suppressed.java",
        """
                package com.acme;

                @SuppressWarnings("all")
                class Suppressed {
                    private int primitive;

                    String value(Integer count) {
                        return "ok";
                    }
                }
                """);

    CheckReport report = runCheck(defaultConfig());

    assertThat(report.rawFindingCount()).isEqualTo(3);
    assertThat(report.inlineSuppressedCount()).isEqualTo(3);
    assertThat(report.newFindings()).isEmpty();
    assertThat(report.activeFindings()).isEmpty();
  }

  @Test
  void methodSuppressWarningsArraySyntaxIsRespected() throws Exception {
    writeSource(
        "src/main/java/com/acme/MethodSuppressed.java",
        """
                package com.acme;

                class MethodSuppressed {
                    @SuppressWarnings({"unchecked", "primitive-boxed-signature"})
                    String value(Integer count) {
                        return "ok";
                    }
                }
                """);

    CheckReport report = runCheck(defaultConfig());

    assertThat(report.rawFindingCount()).isEqualTo(2);
    assertThat(report.inlineSuppressedCount()).isEqualTo(2);
    assertThat(report.newFindings()).isEmpty();
  }

  @Test
  void recordComponentSuppressWarningsSuppressesComponentButNotConstructorParameter()
      throws Exception {
    writeSource(
        "src/main/java/com/acme/RecordSuppressed.java",
        """
                package com.acme;

                record RecordSuppressed(@SuppressWarnings("primitive-boxed-signature") Integer id) {
                }
                """);

    CheckReport report = runCheck(defaultConfig());

    assertThat(report.rawFindingCount()).isEqualTo(2);
    assertThat(report.inlineSuppressedCount()).isEqualTo(1);
    assertThat(report.newFindings())
        .extracting(
            Finding::ownerType, Finding::violationRole, Finding::memberKind, Finding::forbiddenType)
        .containsExactly(
            tuple("RecordSuppressed", "method_parameter_type", "constructor", "java.lang.Integer"));
  }

  @Test
  void failsOnCompilationErrorsWhenConfigured() throws Exception {
    writeSource(
        "src/main/java/com/acme/Broken.java",
        """
                package com.acme;

                class Broken {
                    void value( {
                    }
                }
                """);

    LinterConfig config = defaultConfig();
    config.failOnCompileErrors = true;

    assertThatThrownBy(() -> runCheck(config))
        .isInstanceOf(LinterException.class)
        .hasMessageContaining("Compilation errors detected");
  }

  @Test
  void canIgnoreCompilationErrorsAndStillReportFromValidSources() throws Exception {
    writeSource(
        "src/main/java/com/acme/Valid.java",
        """
                package com.acme;

                class Valid {
                    int value;
                }
                """);

    writeSource(
        "src/main/java/com/acme/Broken.java",
        """
                package com.acme;

                class Broken {
                    void value( {
                    }
                }
                """);

    LinterConfig config = defaultConfig();
    config.failOnCompileErrors = false;

    CheckReport report = runCheck(config);

    assertThat(report.newFindings()).isNotEmpty();
    assertThat(report.newFindings())
        .extracting(Finding::file)
        .anyMatch(file -> file.endsWith("Valid.java"));
  }

  @Test
  void includeAndExcludeGlobsAreApplied() throws Exception {
    writeSource(
        "src/main/java/com/acme/A.java",
        """
                package com.acme;

                class A {
                    int value;
                }
                """);

    writeSource(
        "src/main/java/com/acme/generated/B.java",
        """
                package com.acme.generated;

                class B {
                    int value;
                }
                """);

    LinterConfig config = defaultConfig();
    config.excludeGlobs = List.of("**/generated/**");

    CheckReport report = runCheck(config);

    assertThat(report.newFindings()).hasSize(1);
    assertThat(report.newFindings())
        .extracting(Finding::file)
        .containsExactly("src/main/java/com/acme/A.java");
  }

  @Test
  void findingIdsAreStableWhenLinesMove() throws Exception {
    Path sourceFile =
        writeSource(
            "src/main/java/com/acme/Stable.java",
            """
                package com.acme;

                class Stable {
                    int value;

                    String call(Integer id) {
                        return "ok";
                    }
                }
                """);

    CheckReport first = runCheck(defaultConfig());
    Map<String, String> firstIdsBySemanticKey = toSemanticIdMap(first.newFindings());
    Map<String, Integer> firstLinesBySemanticKey = toSemanticLineMap(first.newFindings());

    Files.writeString(
        sourceFile,
        """
                package com.acme;



                class Stable {


                    int value;


                    String call(Integer id) {
                        return "ok";
                    }
                }
                """);

    CheckReport second = runCheck(defaultConfig());
    Map<String, String> secondIdsBySemanticKey = toSemanticIdMap(second.newFindings());
    Map<String, Integer> secondLinesBySemanticKey = toSemanticLineMap(second.newFindings());

    assertThat(secondIdsBySemanticKey).isEqualTo(firstIdsBySemanticKey);
    assertThat(secondLinesBySemanticKey).isNotEqualTo(firstLinesBySemanticKey);
  }

  @Test
  void userDefinedStringTypeIsNotTreatedAsJavaLangString() throws Exception {
    writeSource(
        "src/main/java/com/acme/CustomTypes.java",
        """
                package com.acme;

                class String {
                }

                class CustomTypes {
                    String value;
                }
                """);

    CheckReport report = runCheck(defaultConfig());

    assertThat(report.newFindings()).isEmpty();
  }

  @Test
  void packageOverrideCanEnablePackagePrivateMethodsForSpecificSubtree() throws Exception {
    writeSource(
        "src/main/java/com/acme/DefaultArea.java",
        """
                package com.acme;

                class DefaultArea {
                    void call(Integer id) {
                    }
                }
                """);

    writeSource(
        "src/main/java/com/acme/special/SpecialArea.java",
        """
                package com.acme.special;

                class SpecialArea {
                    void call(Integer id) {
                    }
                }
                """);

    LinterConfig config = defaultConfig();
    config.visibility.methods.includePackagePrivate = false;

    PackageVisibilityOverride override = new PackageVisibilityOverride();
    override.pattern = "com.acme.special.**";

    VisibilityPolicyOverride methodPolicy = new VisibilityPolicyOverride();
    methodPolicy.includePackagePrivate = true;
    override.methods = methodPolicy;

    config.packageOverrides = List.of(override);

    CheckReport report = runCheck(config);

    assertThat(report.newFindings()).hasSize(1);
    assertThat(report.newFindings().get(0).packageName()).isEqualTo("com.acme.special");
    assertThat(report.newFindings().get(0).memberKind()).isEqualTo("method");
  }

  @Test
  void defaultOwnerAnnotationExclusionsApplyToFieldLikeAndParameters() throws Exception {
    writeSource(
        "src/main/java/com/acme/OwnerExclusions.java",
        """
                package com.acme;

                @interface EnterpriseValueObject {
                }

                @EnterpriseValueObject
                class AnnotatedOwner {
                    int primitiveField;

                    AnnotatedOwner(Integer id) {
                    }

                    String value(Integer count) {
                        return "ok";
                    }
                }

                class PlainOwner {
                    int primitiveField;

                    String value(Integer count) {
                        return "ok";
                    }
                }

                @EnterpriseValueObject
                record AnnotatedRecord(Integer value, String label) {
                }

                record PlainRecord(Integer value, String label) {
                }
                """);

    CheckReport report = runCheck(defaultConfig());

    assertThat(report.newFindings())
        .extracting(
            Finding::ownerType, Finding::violationRole, Finding::memberKind, Finding::forbiddenType)
        .doesNotContain(
            tuple("AnnotatedOwner", "field_type", "field", "int"),
            tuple("AnnotatedOwner", "method_parameter_type", "constructor", "java.lang.Integer"),
            tuple("AnnotatedOwner", "method_parameter_type", "method", "java.lang.Integer"),
            tuple(
                "AnnotatedRecord",
                "record_component_type",
                "record_component",
                "java.lang.Integer"),
            tuple(
                "AnnotatedRecord", "record_component_type", "record_component", "java.lang.String"),
            tuple("AnnotatedRecord", "method_parameter_type", "constructor", "java.lang.Integer"),
            tuple("AnnotatedRecord", "method_parameter_type", "constructor", "java.lang.String"))
        .contains(
            tuple("AnnotatedOwner", "method_return_type", "method", "java.lang.String"),
            tuple("PlainOwner", "field_type", "field", "int"),
            tuple("PlainOwner", "method_parameter_type", "method", "java.lang.Integer"),
            tuple("PlainOwner", "method_return_type", "method", "java.lang.String"),
            tuple("PlainRecord", "record_component_type", "record_component", "java.lang.Integer"),
            tuple("PlainRecord", "record_component_type", "record_component", "java.lang.String"),
            tuple("PlainRecord", "method_parameter_type", "constructor", "java.lang.Integer"),
            tuple("PlainRecord", "method_parameter_type", "constructor", "java.lang.String"));
  }

  @Test
  void recordComponentsRemainVisibleWhenPrivateAndPackagePrivateAreDisabled() throws Exception {
    writeSource(
        "src/main/java/com/acme/OnlyRecord.java",
        """
                package com.acme;

                record OnlyRecord(Integer id) {
                }
                """);

    LinterConfig config = defaultConfig();
    config.visibility.fields.includePrivate = false;
    config.visibility.fields.includePackagePrivate = false;
    config.visibility.methods.includePrivate = false;
    config.visibility.methods.includePackagePrivate = false;

    CheckReport report = runCheck(config);

    assertThat(report.newFindings())
        .extracting(
            Finding::ownerType, Finding::violationRole, Finding::memberKind, Finding::forbiddenType)
        .containsExactly(
            tuple("OnlyRecord", "record_component_type", "record_component", "java.lang.Integer"));
  }

  @Test
  void annotationExclusionsCanBeDisabledIndependentlyPerScope() throws Exception {
    writeSource(
        "src/main/java/com/acme/CustomOwnerScope.java",
        """
                package com.acme;

                @interface EnterpriseValueObject {
                }

                @EnterpriseValueObject
                class OwnerScope {
                    int primitiveField;

                    OwnerScope(Integer id) {
                    }

                    String value(Integer count) {
                        return "ok";
                    }
                }
                """);

    LinterConfig config = defaultConfig();
    config.annotatedTypeExclusions.fieldLikeOwnerAnnotations = List.of();
    config.annotatedTypeExclusions.parameterOwnerAnnotations = List.of();

    CheckReport report = runCheck(config);

    assertThat(report.newFindings())
        .extracting(
            Finding::ownerType, Finding::violationRole, Finding::memberKind, Finding::forbiddenType)
        .contains(
            tuple("OwnerScope", "field_type", "field", "int"),
            tuple("OwnerScope", "method_parameter_type", "constructor", "java.lang.Integer"),
            tuple("OwnerScope", "method_parameter_type", "method", "java.lang.Integer"),
            tuple("OwnerScope", "method_return_type", "method", "java.lang.String"));
  }

  @Test
  void ownerAnnotationExclusionsSupportFullyQualifiedConfiguredNames() throws Exception {
    writeSource(
        "src/main/java/com/acme/annotations/DomainMarker.java",
        """
                package com.acme.annotations;

                public @interface DomainMarker {
                }
                """);

    writeSource(
        "src/main/java/com/acme/FqcnOwner.java",
        """
                package com.acme;

                import com.acme.annotations.DomainMarker;

                @DomainMarker
                class FqcnOwner {
                    int primitiveField;

                    FqcnOwner(Integer id) {
                    }
                }
                """);

    LinterConfig config = defaultConfig();
    config.annotatedTypeExclusions.fieldLikeOwnerAnnotations =
        List.of("com.acme.annotations.DomainMarker");
    config.annotatedTypeExclusions.parameterOwnerAnnotations =
        List.of("com.acme.annotations.DomainMarker");

    CheckReport report = runCheck(config);

    assertThat(report.newFindings()).extracting(Finding::ownerType).doesNotContain("FqcnOwner");
  }

  private CheckReport runCheck(LinterConfig config) throws Exception {
    return new LinterEngine().check(tempDir, config);
  }

  private LinterConfig defaultConfig() {
    LinterConfig config = new LinterConfig();
    config.baseline.enabled = false;
    return config;
  }

  private Path writeSource(String relativePath, String content) throws IOException {
    Path file = tempDir.resolve(relativePath);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content);
    return file;
  }

  private Map<String, String> toSemanticIdMap(List<Finding> findings) {
    Map<String, String> mapping = new HashMap<>();
    for (Finding finding : findings) {
      mapping.put(semanticKey(finding), finding.findingId());
    }
    return mapping;
  }

  private Map<String, Integer> toSemanticLineMap(List<Finding> findings) {
    Map<String, Integer> mapping = new HashMap<>();
    for (Finding finding : findings) {
      mapping.put(semanticKey(finding), finding.line());
    }
    return mapping;
  }

  private String semanticKey(Finding finding) {
    return String.join(
        "|",
        finding.file(),
        finding.memberSignature(),
        finding.violationRole(),
        finding.forbiddenType());
  }
}
