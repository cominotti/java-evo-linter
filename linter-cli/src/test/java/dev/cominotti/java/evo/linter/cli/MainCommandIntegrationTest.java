// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class MainCommandIntegrationTest {
  @TempDir Path tempDir;

  @Test
  void checkCommandReportsFindingsWithHumanOutput() throws Exception {
    writeSource(defaultSource());

    CommandResult result =
        runCommand("check", "--project-root", tempDir.toString(), "--format", "human");

    Assertions.assertThat(result.exitCode()).isEqualTo(1);
    Assertions.assertThat(result.stdout()).contains("New findings");
    Assertions.assertThat(result.stdout()).contains("[primitive-boxed-signature]");
    Assertions.assertThat(result.stdout()).contains("New findings: 3");
    Assertions.assertThat(result.stderr()).isBlank();
  }

  @Test
  void checkCommandWritesJsonlWhenRequested() throws Exception {
    writeSource(defaultSource());
    Path jsonl = tempDir.resolve("reports/findings.jsonl");

    CommandResult result =
        runCommand(
            "check",
            "--project-root",
            tempDir.toString(),
            "--format",
            "jsonl",
            "--jsonl-path",
            jsonl.toString());

    Assertions.assertThat(result.exitCode()).isEqualTo(1);
    Assertions.assertThat(result.stdout()).doesNotContain("New findings");
    Assertions.assertThat(Files.exists(jsonl)).isTrue();
    Assertions.assertThat(Files.readAllLines(jsonl)).hasSize(3);
  }

  @Test
  void checkCommandDisablePrivateAffectsSemantics() throws Exception {
    writeSource(
        """
                package com.acme;

                class Example {
                    private int privateValue;
                    public int publicValue;
                }
                """);

    CommandResult defaultResult =
        runCommand("check", "--project-root", tempDir.toString(), "--format", "human");

    CommandResult disabledResult =
        runCommand(
            "check",
            "--project-root",
            tempDir.toString(),
            "--format",
            "human",
            "--disable-private");

    Assertions.assertThat(extractNewFindingsCount(defaultResult.stdout())).isEqualTo(2);
    Assertions.assertThat(extractNewFindingsCount(disabledResult.stdout())).isEqualTo(1);
  }

  @Test
  void baselineGenerateAndCheckIntegrationFlow() throws Exception {
    writeSource(defaultSource());
    Path baselinePath = tempDir.resolve(".baseline.jsonl");

    CommandResult generated =
        runCommand(
            "baseline", "generate",
            "--project-root", tempDir.toString(),
            "--baseline", baselinePath.toString());

    Assertions.assertThat(generated.exitCode()).isZero();
    Assertions.assertThat(Files.exists(baselinePath)).isTrue();

    CommandResult checked =
        runCommand(
            "check",
            "--project-root",
            tempDir.toString(),
            "--baseline",
            baselinePath.toString(),
            "--format",
            "human");

    Assertions.assertThat(checked.exitCode()).isZero();
    Assertions.assertThat(checked.stdout()).contains("New findings: 0");
  }

  @Test
  void baselineDiffFailsOnStaleWhenRequested() throws Exception {
    writeSource(defaultSource());
    Path baselinePath = tempDir.resolve(".baseline.jsonl");

    CommandResult generated =
        runCommand(
            "baseline", "generate",
            "--project-root", tempDir.toString(),
            "--baseline", baselinePath.toString());
    Assertions.assertThat(generated.exitCode()).isZero();

    writeSource(
        """
                package com.acme;

                class Example {
                    Value value;
                }

                class Value {
                }
                """);

    CommandResult diffResult =
        runCommand(
            "baseline",
            "diff",
            "--project-root",
            tempDir.toString(),
            "--baseline",
            baselinePath.toString(),
            "--fail-on-stale");

    Assertions.assertThat(diffResult.exitCode()).isEqualTo(1);
    Assertions.assertThat(diffResult.stdout()).contains("Stale baseline entries");
  }

  @Test
  void returnsErrorCodeTwoForMissingConfigFile() throws Exception {
    writeSource(defaultSource());

    CommandResult result =
        runCommand(
            "check",
            "--project-root",
            tempDir.toString(),
            "--config",
            tempDir.resolve("missing.toml").toString());

    Assertions.assertThat(result.exitCode()).isEqualTo(2);
    Assertions.assertThat(result.stderr()).contains("Config file not found");
  }

  private void writeSource(String source) throws IOException {
    Path file = tempDir.resolve("src/main/java/com/acme/Example.java");
    Files.createDirectories(file.getParent());
    Files.writeString(file, source);
  }

  private String defaultSource() {
    return """
                package com.acme;

                class Example {
                    int primitive;

                    String value(Integer count) {
                        return "ok";
                    }
                }
                """;
  }

  private CommandResult runCommand(String... args) {
    ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
    ByteArrayOutputStream stderrBuffer = new ByteArrayOutputStream();
    var commandLine = new CommandLine(new Main());
    var stdout =
        new PrintWriter(new OutputStreamWriter(stdoutBuffer, StandardCharsets.UTF_8), true);
    var stderr =
        new PrintWriter(new OutputStreamWriter(stderrBuffer, StandardCharsets.UTF_8), true);
    commandLine.setOut(stdout);
    commandLine.setErr(stderr);
    var exitCode = commandLine.execute(args);
    stdout.flush();
    stderr.flush();
    return new CommandResult(
        exitCode,
        stdoutBuffer.toString(StandardCharsets.UTF_8),
        stderrBuffer.toString(StandardCharsets.UTF_8));
  }

  private int extractNewFindingsCount(String output) {
    Matcher matcher = Pattern.compile("New findings: (\\d+)").matcher(output);
    Assertions.assertThat(matcher.find()).isTrue();
    return Integer.parseInt(matcher.group(1));
  }

  private record CommandResult(int exitCode, String stdout, String stderr) {}
}
