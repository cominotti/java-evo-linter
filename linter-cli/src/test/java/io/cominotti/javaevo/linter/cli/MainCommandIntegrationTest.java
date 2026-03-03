package io.cominotti.javaevo.linter.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    assertThat(result.exitCode()).isEqualTo(1);
    assertThat(result.stdout()).contains("New findings");
    assertThat(result.stdout()).contains("[primitive-boxed-signature]");
    assertThat(result.stdout()).contains("New findings: 3");
    assertThat(result.stderr()).isBlank();
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

    assertThat(result.exitCode()).isEqualTo(1);
    assertThat(result.stdout()).doesNotContain("New findings");
    assertThat(Files.exists(jsonl)).isTrue();
    assertThat(Files.readAllLines(jsonl)).hasSize(3);
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

    assertThat(extractNewFindingsCount(defaultResult.stdout())).isEqualTo(2);
    assertThat(extractNewFindingsCount(disabledResult.stdout())).isEqualTo(1);
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

    assertThat(generated.exitCode()).isZero();
    assertThat(Files.exists(baselinePath)).isTrue();

    CommandResult checked =
        runCommand(
            "check",
            "--project-root",
            tempDir.toString(),
            "--baseline",
            baselinePath.toString(),
            "--format",
            "human");

    assertThat(checked.exitCode()).isZero();
    assertThat(checked.stdout()).contains("New findings: 0");
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
    assertThat(generated.exitCode()).isZero();

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

    assertThat(diffResult.exitCode()).isEqualTo(1);
    assertThat(diffResult.stdout()).contains("Stale baseline entries");
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

    assertThat(result.exitCode()).isEqualTo(2);
    assertThat(result.stderr()).contains("Config file not found");
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

    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;

    try {
      System.setOut(new PrintStream(stdoutBuffer, true, StandardCharsets.UTF_8));
      System.setErr(new PrintStream(stderrBuffer, true, StandardCharsets.UTF_8));

      int exitCode = new CommandLine(new Main()).execute(args);
      return new CommandResult(
          exitCode,
          stdoutBuffer.toString(StandardCharsets.UTF_8),
          stderrBuffer.toString(StandardCharsets.UTF_8));
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
  }

  private int extractNewFindingsCount(String output) {
    Matcher matcher = Pattern.compile("New findings: (\\d+)").matcher(output);
    assertThat(matcher.find()).isTrue();
    return Integer.parseInt(matcher.group(1));
  }

  private record CommandResult(int exitCode, String stdout, String stderr) {}
}
