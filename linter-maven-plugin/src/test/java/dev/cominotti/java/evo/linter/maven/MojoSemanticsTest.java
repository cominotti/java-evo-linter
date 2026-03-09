// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MojoSemanticsTest {
  @TempDir Path tempDir;

  @Test
  void checkMojoFailsBuildWhenNewFindingsExist() throws Exception {
    Path sourceRoot = createDefaultProjectLayout();
    writeJava(
        sourceRoot,
        "com/acme/Example.java",
        """
                package com.acme;

                class Example {
                    int value;
                }
                """);

    MavenProject project = newProject(sourceRoot);

    CheckMojo mojo = new CheckMojo();
    mojo.project = project;
    mojo.configFile = tempDir.resolve("missing.toml").toFile();
    mojo.baselineFile = tempDir.resolve("baseline.jsonl").toFile();
    mojo.jsonlPath = tempDir.resolve("target/findings.jsonl").toFile();
    mojo.failOnNewFindings = true;

    Assertions.assertThatThrownBy(mojo::execute)
        .isInstanceOf(MojoFailureException.class)
        .hasMessageContaining("java-evo-linter found");
  }

  @Test
  void checkMojoCanPassAndWriteJsonlWhenFailOnNewFindingsIsFalse() throws Exception {
    Path sourceRoot = createDefaultProjectLayout();
    writeJava(
        sourceRoot,
        "com/acme/Example.java",
        """
                package com.acme;

                class Example {
                    int value;
                }
                """);

    MavenProject project = newProject(sourceRoot);

    Path jsonlPath = tempDir.resolve("target/findings.jsonl");

    CheckMojo mojo = new CheckMojo();
    mojo.project = project;
    mojo.configFile = tempDir.resolve("missing.toml").toFile();
    mojo.baselineFile = tempDir.resolve("baseline.jsonl").toFile();
    mojo.jsonlPath = jsonlPath.toFile();
    mojo.failOnNewFindings = false;

    mojo.execute();

    Assertions.assertThat(Files.exists(jsonlPath)).isTrue();
    Assertions.assertThat(Files.readAllLines(jsonlPath)).hasSize(1);
  }

  @Test
  void baselineDiffMojoCanFailOnStaleEntries() throws Exception {
    Path sourceRoot = createDefaultProjectLayout();
    Path sourceFile = sourceRoot.resolve("com/acme/Example.java");

    writeJava(
        sourceRoot,
        "com/acme/Example.java",
        """
                package com.acme;

                class Example {
                    int value;
                }
                """);

    MavenProject project = newProject(sourceRoot);
    Path baselinePath = tempDir.resolve("baseline.jsonl");

    BaselineGenerateMojo generateMojo = new BaselineGenerateMojo();
    generateMojo.project = project;
    generateMojo.configFile = tempDir.resolve("missing.toml").toFile();
    generateMojo.baselineFile = baselinePath.toFile();
    generateMojo.execute();

    Files.writeString(
        sourceFile,
        """
                package com.acme;

                class Example {
                    Value value;
                }

                class Value {
                }
                """);

    BaselineDiffMojo diffMojo = new BaselineDiffMojo();
    diffMojo.project = project;
    diffMojo.configFile = tempDir.resolve("missing.toml").toFile();
    diffMojo.baselineFile = baselinePath.toFile();
    diffMojo.failOnMissing = false;
    diffMojo.failOnStale = true;

    Assertions.assertThatThrownBy(diffMojo::execute)
        .isInstanceOf(MojoFailureException.class)
        .hasMessageContaining("stale entries");
  }

  @Test
  void disablePrivateOptionAffectsMojoSemantics() throws Exception {
    Path sourceRoot = createDefaultProjectLayout();
    writeJava(
        sourceRoot,
        "com/acme/Example.java",
        """
                package com.acme;

                class Example {
                    private int hidden;
                }
                """);

    MavenProject project = newProject(sourceRoot);

    CheckMojo mojo = new CheckMojo();
    mojo.project = project;
    mojo.configFile = tempDir.resolve("missing.toml").toFile();
    mojo.baselineFile = tempDir.resolve("baseline.jsonl").toFile();
    mojo.jsonlPath = tempDir.resolve("target/findings.jsonl").toFile();
    mojo.failOnNewFindings = true;
    mojo.disablePrivate = true;

    mojo.execute();

    Assertions.assertThat(Files.readAllLines(mojo.jsonlPath.toPath())).isEmpty();
  }

  private Path createDefaultProjectLayout() throws IOException {
    Path sourceRoot = tempDir.resolve("src/main/java");
    Files.createDirectories(sourceRoot);

    Path pom = tempDir.resolve("pom.xml");
    if (!Files.exists(pom)) {
      Files.writeString(
          pom,
          """
                    <project xmlns=\"http://maven.apache.org/POM/4.0.0\"
                             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"
                             xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>io.cominotti</groupId>
                      <artifactId>sample</artifactId>
                      <version>1.0.0</version>
                    </project>
                    """);
    }

    return sourceRoot;
  }

  private MavenProject newProject(Path sourceRoot) {
    return new StubMavenProject(tempDir, List.of(sourceRoot.toString()), List.of());
  }

  private void writeJava(Path sourceRoot, String relativePath, String source) throws IOException {
    Path file = sourceRoot.resolve(relativePath);
    Files.createDirectories(file.getParent());
    Files.writeString(file, source);
  }

  private static final class StubMavenProject extends MavenProject {
    private final Path basedir;
    private final List<String> compileSourceRoots;
    private final List<String> compileClasspathElements;

    private StubMavenProject(
        Path basedir, List<String> compileSourceRoots, List<String> compileClasspathElements) {
      this.basedir = basedir;
      this.compileSourceRoots = compileSourceRoots;
      this.compileClasspathElements = compileClasspathElements;
    }

    @Override
    public java.io.File getBasedir() {
      return basedir.toFile();
    }

    @Override
    public List<String> getCompileSourceRoots() {
      return compileSourceRoots;
    }

    @Override
    public List<String> getCompileClasspathElements() throws DependencyResolutionRequiredException {
      return compileClasspathElements;
    }
  }
}
