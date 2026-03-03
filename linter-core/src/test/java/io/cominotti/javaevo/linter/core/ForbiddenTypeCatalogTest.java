package io.cominotti.javaevo.linter.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ForbiddenTypeCatalogTest {
  @Test
  void normalizesPrimitiveAndSimpleNames() {
    assertThat(ForbiddenTypeCatalog.normalizeConfiguredType("INT")).isEqualTo("int");
    assertThat(ForbiddenTypeCatalog.normalizeConfiguredType("Integer"))
        .isEqualTo("java.lang.Integer");
    assertThat(ForbiddenTypeCatalog.normalizeConfiguredType("java.lang.Integer"))
        .isEqualTo("java.lang.Integer");
    assertThat(ForbiddenTypeCatalog.normalizeConfiguredType("String"))
        .isEqualTo("java.lang.String");
    assertThat(ForbiddenTypeCatalog.normalizeConfiguredType("com.acme.Value"))
        .isEqualTo("com.acme.Value");
  }

  @Test
  void rejectsNullAndBlankTypes() {
    assertThatThrownBy(() -> ForbiddenTypeCatalog.normalizeConfiguredType(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be null");

    assertThatThrownBy(() -> ForbiddenTypeCatalog.normalizeConfiguredType("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be blank");
  }

  @Test
  void defaultCatalogContainsExpectedBuiltins() {
    List<String> defaults = ForbiddenTypeCatalog.defaultForbiddenTypes();

    assertThat(defaults)
        .contains("int", "double", "char")
        .contains("java.lang.Integer", "java.lang.String", "java.lang.Boolean");
    assertThat(defaults).hasSize(17);
  }

  @Test
  void configNormalizationCanonicalizesAndDeduplicatesForbiddenTypes() {
    LinterConfig config = new LinterConfig();
    config.forbiddenTypes =
        new ArrayList<>(
            List.of("int", "INT", "Integer", "java.lang.Integer", "String", "java.lang.String"));

    config.normalize();

    assertThat(config.forbiddenTypes)
        .containsExactly("int", "java.lang.Integer", "java.lang.String");
  }
}
