// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LinterConfigLoaderTest {
  @TempDir Path tempDir;

  @Test
  void loadsDefaultsWhenConfigIsMissing() throws Exception {
    LinterConfigLoader loader = new LinterConfigLoader();

    LinterConfig config = loader.load(null, tempDir);

    Assertions.assertThat(config.sourceRoots()).containsExactly("src/main/java");
    Assertions.assertThat(config.includeGlobs()).containsExactly("**/*.java");
    Assertions.assertThat(config.annotatedTypeExclusions().fieldLikeOwnerAnnotations())
        .containsExactly("EvoType");
    Assertions.assertThat(config.annotatedTypeExclusions().parameterOwnerAnnotations())
        .containsExactly("EvoType");
    Assertions.assertThat(config.output().format()).isEqualTo(OutputFormat.BOTH);
    Assertions.assertThat(config.baseline().path()).isEqualTo(".java-evo-linter-baseline.jsonl");
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

    Assertions.assertThat(config.sourceRoots()).containsExactly("app/src/main/java");
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

    var overrides =
        new LinterConfigOverrides()
            .withBaselinePath(Path.of("custom/baseline.jsonl"))
            .withOutputFormat(OutputFormat.JSONL)
            .withJsonlPath(Path.of("custom/findings.jsonl"))
            .withSourceRoots(List.of(Path.of("src/alt/main/java")))
            .withClasspathEntries(List.of(Path.of("build/classes")))
            .withIncludePrivateFields(Boolean.TRUE)
            .withIncludePackagePrivateMethods(Boolean.TRUE);

    LinterConfig effective = loader.applyOverrides(loaded, overrides, tempDir);

    Assertions.assertThat(effective.baseline().path())
        .isEqualTo(tempDir.resolve("custom/baseline.jsonl").toString());
    Assertions.assertThat(effective.output().format()).isEqualTo(OutputFormat.JSONL);
    Assertions.assertThat(effective.output().jsonlPath())
        .isEqualTo(tempDir.resolve("custom/findings.jsonl").toString());
    Assertions.assertThat(effective.sourceRoots())
        .containsExactly(tempDir.resolve("src/alt/main/java").toString());
    Assertions.assertThat(effective.classpath())
        .containsExactly(tempDir.resolve("build/classes").toString());
    Assertions.assertThat(effective.visibility().fields().includePrivate()).isTrue();
    Assertions.assertThat(effective.visibility().methods().includePackagePrivate()).isTrue();
  }

  @Test
  void normalizesAnnotationExclusionListsAndPreservesExplicitEmptyArrays() throws Exception {
    Path configPath = tempDir.resolve(".java-evo-linter.toml");
    Files.writeString(
        configPath,
        """
                [annotatedTypeExclusions]
                fieldLikeOwnerAnnotations = [
                  "@EvoType",
                  " com.acme.CustomMarker ",
                  "com.acme.CustomMarker"
                ]
                parameterOwnerAnnotations = []
                """);

    LinterConfigLoader loader = new LinterConfigLoader();
    LinterConfig config = loader.load(configPath, tempDir);

    Assertions.assertThat(config.annotatedTypeExclusions().fieldLikeOwnerAnnotations())
        .containsExactly("EvoType", "com.acme.CustomMarker");
    Assertions.assertThat(config.annotatedTypeExclusions().parameterOwnerAnnotations()).isEmpty();
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

    Assertions.assertThatThrownBy(() -> loader.load(configPath, tempDir))
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

    Assertions.assertThatThrownBy(() -> loader.load(configPath, tempDir))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("YAML config is no longer supported");
  }

  @Test
  void normalizePathSupportsRelativeAndAbsoluteValues() {
    Path relative = LinterConfigLoader.normalizePath(tempDir, Path.of("reports/findings.jsonl"));
    Path absoluteInput = tempDir.resolve("absolute/value.txt").toAbsolutePath();
    Path absolute = LinterConfigLoader.normalizePath(tempDir, absoluteInput);

    Assertions.assertThat(relative)
        .isEqualTo(tempDir.resolve("reports/findings.jsonl").normalize());
    Assertions.assertThat(absolute).isEqualTo(absoluteInput.normalize());
  }
}
