// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ForbiddenTypeCatalogTest {
  @Test
  void normalizesPrimitiveAndSimpleNames() {
    Assertions.assertThat(ForbiddenTypeCatalog.normalizeConfiguredType("INT")).isEqualTo("int");
    Assertions.assertThat(ForbiddenTypeCatalog.normalizeConfiguredType("Integer"))
        .isEqualTo("java.lang.Integer");
    Assertions.assertThat(ForbiddenTypeCatalog.normalizeConfiguredType("java.lang.Integer"))
        .isEqualTo("java.lang.Integer");
    Assertions.assertThat(ForbiddenTypeCatalog.normalizeConfiguredType("String"))
        .isEqualTo("java.lang.String");
    Assertions.assertThat(ForbiddenTypeCatalog.normalizeConfiguredType("com.acme.Value"))
        .isEqualTo("com.acme.Value");
  }

  @Test
  void rejectsNullAndBlankTypes() {
    Assertions.assertThatThrownBy(() -> ForbiddenTypeCatalog.normalizeConfiguredType(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be null");

    Assertions.assertThatThrownBy(() -> ForbiddenTypeCatalog.normalizeConfiguredType("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be blank");
  }

  @Test
  void defaultCatalogContainsExpectedBuiltins() {
    List<String> defaults = ForbiddenTypeCatalog.defaultForbiddenTypes();

    Assertions.assertThat(defaults)
        .contains("int", "double", "char")
        .contains("java.lang.Integer", "java.lang.String", "java.lang.Boolean")
        .hasSize(17);
  }

  @Test
  void configNormalizationCanonicalizesAndDeduplicatesForbiddenTypes() {
    var config =
        new LinterConfig()
            .withForbiddenTypes(
                new ArrayList<>(
                    List.of(
                        "int",
                        "INT",
                        "Integer",
                        "java.lang.Integer",
                        "String",
                        "java.lang.String")));

    Assertions.assertThat(config.forbiddenTypes())
        .containsExactly("int", "java.lang.Integer", "java.lang.String");
  }
}
