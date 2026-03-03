package io.cominotti.javaevo.linter.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReporterOutputTest {
  @TempDir Path tempDir;

  @Test
  void humanReporterIncludesSummaryAndStaleSection() {
    Finding finding = finding("id-1", "src/main/java/com/acme/A.java", 12);
    CheckReport report =
        new CheckReport(
            List.of(finding),
            List.of(finding),
            1,
            0,
            0,
            List.of(
                new BaselineEntry(
                    "stale-id", RuleIds.PRIMITIVE_BOXED_SIGNATURE, null, null, null)));

    String output = new HumanReporter().renderCheckReport(report);

    assertThat(output).contains("New findings");
    assertThat(output).contains("Summary");
    assertThat(output).contains("Raw findings: 1");
    assertThat(output).contains("Stale baseline entries");
    assertThat(output).contains("stale-id");
  }

  @Test
  void jsonlReporterWritesParseableFindingRecords() throws Exception {
    Finding finding = finding("id-2", "src/main/java/com/acme/B.java", 30);
    JsonlReporter reporter = new JsonlReporter();

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    reporter.writeToStream(outputStream, List.of(finding));

    String line = outputStream.toString(StandardCharsets.UTF_8).trim();
    assertThat(line).isNotBlank();

    ObjectMapper mapper = new ObjectMapper();
    var node = mapper.readTree(line);
    assertThat(node.get("rule_id").asText()).isEqualTo(RuleIds.PRIMITIVE_BOXED_SIGNATURE);
    assertThat(node.get("finding_id").asText()).isEqualTo("id-2");

    Path jsonlFile = tempDir.resolve("reports/findings.jsonl");
    reporter.writeToPath(jsonlFile, List.of(finding));
    assertThat(Files.exists(jsonlFile)).isTrue();
    assertThat(Files.readAllLines(jsonlFile)).hasSize(1);
  }

  private Finding finding(String id, String file, int line) {
    return new Finding(
        Finding.SCHEMA_VERSION,
        RuleIds.PRIMITIVE_BOXED_SIGNATURE,
        id,
        "error",
        file,
        line,
        1,
        "com.acme",
        "Sample",
        "field",
        "Sample#value",
        "private",
        "field_type",
        "int",
        "int",
        "Forbidden primitive/boxed type in production signature",
        "Replace with a domain value object");
  }
}
