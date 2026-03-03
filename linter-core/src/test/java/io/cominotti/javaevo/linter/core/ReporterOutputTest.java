// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.assertj.core.api.Assertions;
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

    Assertions.assertThat(output)
        .contains("New findings")
        .contains("Summary")
        .contains("Raw findings: 1")
        .contains("Stale baseline entries")
        .contains("stale-id");
  }

  @Test
  void jsonlReporterWritesParseableFindingRecords() throws Exception {
    Finding finding = finding("id-2", "src/main/java/com/acme/B.java", 30);
    JsonlReporter reporter = new JsonlReporter();

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    reporter.writeToStream(outputStream, List.of(finding));

    String line = outputStream.toString(StandardCharsets.UTF_8).trim();
    Assertions.assertThat(line).isNotBlank();

    ObjectMapper mapper = new ObjectMapper();
    var node = mapper.readTree(line);
    Assertions.assertThat(node.get("rule_id").asText())
        .isEqualTo(RuleIds.PRIMITIVE_BOXED_SIGNATURE);
    Assertions.assertThat(node.get("finding_id").asText()).isEqualTo("id-2");

    Path jsonlFile = tempDir.resolve("reports/findings.jsonl");
    reporter.writeToPath(jsonlFile, List.of(finding));
    Assertions.assertThat(Files.exists(jsonlFile)).isTrue();
    Assertions.assertThat(Files.readAllLines(jsonlFile)).hasSize(1);
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
