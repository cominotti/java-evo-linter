package io.cominotti.javaevo.linter.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BaselineStoreTest {
  @TempDir Path tempDir;

  @Test
  void writesAndReadsEntriesInDeterministicOrder() throws Exception {
    BaselineStore store = new BaselineStore();
    Path baselinePath = tempDir.resolve("baseline.jsonl");

    List<BaselineEntry> entries =
        List.of(
            new BaselineEntry("b-id", RuleIds.PRIMITIVE_BOXED_SIGNATURE, null, null, null),
            new BaselineEntry(
                "a-id", RuleIds.PRIMITIVE_BOXED_SIGNATURE, "why", "owner", "2030-01-01"));

    store.writeEntries(baselinePath, entries);
    List<BaselineEntry> readEntries = store.readEntries(baselinePath);

    assertThat(readEntries).extracting(BaselineEntry::findingId).containsExactly("a-id", "b-id");
    assertThat(store.readFindingIds(baselinePath)).containsExactly("a-id", "b-id");
  }

  @Test
  void toEntriesDeduplicatesByFindingId() {
    BaselineStore store = new BaselineStore();

    Finding duplicateA = finding("id-1", "a/A.java");
    Finding duplicateB = finding("id-1", "a/B.java");
    Finding unique = finding("id-2", "a/C.java");

    List<BaselineEntry> entries = store.toEntries(List.of(duplicateA, duplicateB, unique));

    assertThat(entries).extracting(BaselineEntry::findingId).containsExactly("id-1", "id-2");
  }

  @Test
  void rejectsInvalidJsonlContent() throws Exception {
    BaselineStore store = new BaselineStore();
    Path baselinePath = tempDir.resolve("invalid.jsonl");
    Files.writeString(baselinePath, "{not-json}\n");

    assertThatThrownBy(() -> store.readEntries(baselinePath))
        .isInstanceOf(LinterException.class)
        .hasMessageContaining("Invalid baseline JSONL");
  }

  @Test
  void rejectsEntriesMissingRequiredFields() throws Exception {
    BaselineStore store = new BaselineStore();
    Path baselinePath = tempDir.resolve("missing-fields.jsonl");
    Files.writeString(
        baselinePath,
        """
                {"rule_id":"primitive-boxed-signature"}
                """);

    assertThatThrownBy(() -> store.readEntries(baselinePath))
        .isInstanceOf(LinterException.class)
        .hasMessageContaining("missing finding_id");
  }

  private Finding finding(String id, String file) {
    return new Finding(
        Finding.SCHEMA_VERSION,
        RuleIds.PRIMITIVE_BOXED_SIGNATURE,
        id,
        "error",
        file,
        10,
        2,
        "com.acme",
        "Example",
        "field",
        "Example#value",
        "public",
        "field_type",
        "int",
        "int",
        "Forbidden primitive/boxed type in production signature",
        "Replace with a domain value object");
  }
}
