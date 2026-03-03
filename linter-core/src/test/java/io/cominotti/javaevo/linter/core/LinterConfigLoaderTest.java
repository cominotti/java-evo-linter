package io.cominotti.javaevo.linter.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LinterConfigLoaderTest {
  @TempDir Path tempDir;

  @Test
  void loadsDefaultsWhenConfigIsMissing() throws Exception {
    LinterConfigLoader loader = new LinterConfigLoader();

    LinterConfig config = loader.load(null, tempDir);

    assertThat(config.sourceRoots).containsExactly("src/main/java");
    assertThat(config.includeGlobs).containsExactly("**/*.java");
    assertThat(config.annotatedTypeExclusions.fieldLikeOwnerAnnotations)
        .containsExactly("EnterpriseValueObject");
    assertThat(config.annotatedTypeExclusions.parameterOwnerAnnotations)
        .containsExactly("EnterpriseValueObject");
    assertThat(config.output.format).isEqualTo(OutputFormat.BOTH);
    assertThat(config.baseline.path).isEqualTo(".java-evo-linter-baseline.jsonl");
  }

  @Test
  void autoDiscoversDefaultTomlConfigWhenPresent() throws Exception {
    Path configPath = tempDir.resolve(".java-evo-linter.toml");
    Files.writeString(
        configPath,
        """
                sourceRoots = ["app/src/main/java"]
                """);

    LinterConfigLoader loader = new LinterConfigLoader();
    LinterConfig config = loader.load(null, tempDir);

    assertThat(config.sourceRoots).containsExactly("app/src/main/java");
  }

  @Test
  void loadsTomlAndAppliesOverridesWithNormalization() throws Exception {
    Path configPath = tempDir.resolve(".java-evo-linter.toml");
    Files.writeString(
        configPath,
        """
                sourceRoots = ["app/src/main/java"]

                [baseline]
                path = "baseline/original.jsonl"

                [output]
                format = "human"
                jsonlPath = "reports/original.jsonl"

                [visibility.fields]
                includePrivate = false

                [visibility.methods]
                includePackagePrivate = false
                """);

    LinterConfigLoader loader = new LinterConfigLoader();
    LinterConfig loaded = loader.load(configPath, tempDir);

    LinterConfigOverrides overrides = new LinterConfigOverrides();
    overrides.baselinePath = Path.of("custom/baseline.jsonl");
    overrides.outputFormat = OutputFormat.JSONL;
    overrides.jsonlPath = Path.of("custom/findings.jsonl");
    overrides.sourceRoots = List.of(Path.of("src/alt/main/java"));
    overrides.classpathEntries = List.of(Path.of("build/classes"));
    overrides.includePrivateFields = true;
    overrides.includePackagePrivateMethods = true;

    LinterConfig effective = loader.applyOverrides(loaded, overrides, tempDir);

    assertThat(effective.baseline.path)
        .isEqualTo(tempDir.resolve("custom/baseline.jsonl").toString());
    assertThat(effective.output.format).isEqualTo(OutputFormat.JSONL);
    assertThat(effective.output.jsonlPath)
        .isEqualTo(tempDir.resolve("custom/findings.jsonl").toString());
    assertThat(effective.sourceRoots)
        .containsExactly(tempDir.resolve("src/alt/main/java").toString());
    assertThat(effective.classpath).containsExactly(tempDir.resolve("build/classes").toString());
    assertThat(effective.visibility.fields.includePrivate).isTrue();
    assertThat(effective.visibility.methods.includePackagePrivate).isTrue();
  }

  @Test
  void normalizesAnnotationExclusionListsAndPreservesExplicitEmptyArrays() throws Exception {
    Path configPath = tempDir.resolve(".java-evo-linter.toml");
    Files.writeString(
        configPath,
        """
                [annotatedTypeExclusions]
                fieldLikeOwnerAnnotations = [
                  "@EnterpriseValueObject",
                  " com.acme.CustomMarker ",
                  "com.acme.CustomMarker"
                ]
                parameterOwnerAnnotations = []
                """);

    LinterConfigLoader loader = new LinterConfigLoader();
    LinterConfig config = loader.load(configPath, tempDir);

    assertThat(config.annotatedTypeExclusions.fieldLikeOwnerAnnotations)
        .containsExactly("EnterpriseValueObject", "com.acme.CustomMarker");
    assertThat(config.annotatedTypeExclusions.parameterOwnerAnnotations).isEmpty();
  }

  @Test
  void rejectsUnknownConfigProperties() throws Exception {
    Path configPath = tempDir.resolve("bad.toml");
    Files.writeString(
        configPath,
        """
                sourceRoots = ["src/main/java"]
                unknownField = true
                """);

    LinterConfigLoader loader = new LinterConfigLoader();

    assertThatThrownBy(() -> loader.load(configPath, tempDir))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("unknownField");
  }

  @Test
  void rejectsYamlConfigFiles() throws Exception {
    Path configPath = tempDir.resolve("legacy.yml");
    Files.writeString(
        configPath,
        """
                sourceRoots:
                  - src/main/java
                """);

    LinterConfigLoader loader = new LinterConfigLoader();

    assertThatThrownBy(() -> loader.load(configPath, tempDir))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("YAML config is no longer supported");
  }

  @Test
  void normalizePathSupportsRelativeAndAbsoluteValues() {
    Path relative = LinterConfigLoader.normalizePath(tempDir, Path.of("reports/findings.jsonl"));
    Path absoluteInput = tempDir.resolve("absolute/value.txt").toAbsolutePath();
    Path absolute = LinterConfigLoader.normalizePath(tempDir, absoluteInput);

    assertThat(relative).isEqualTo(tempDir.resolve("reports/findings.jsonl").normalize());
    assertThat(absolute).isEqualTo(absoluteInput.normalize());
  }
}
