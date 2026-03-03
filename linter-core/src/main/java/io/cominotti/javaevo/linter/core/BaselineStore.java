// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BaselineStore {
  private final ObjectMapper mapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

  public List<BaselineEntry> readEntries(Path baselinePath) throws LinterException {
    if (!Files.exists(baselinePath)) {
      return List.of();
    }

    var entries = new ArrayList<BaselineEntry>();
    try (BufferedReader reader = Files.newBufferedReader(baselinePath, StandardCharsets.UTF_8)) {
      String line;
      var lineNumber = 0;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        var trimmed = line.trim();
        if (trimmed.isEmpty()) {
          continue;
        }
        var entry = parseEntry(trimmed, baselinePath, lineNumber);

        if (entry.findingId() == null || entry.findingId().isBlank()) {
          throw new LinterException(
              "Baseline entry missing finding_id at " + baselinePath + ":" + lineNumber);
        }
        if (entry.ruleId() == null || entry.ruleId().isBlank()) {
          throw new LinterException(
              "Baseline entry missing rule_id at " + baselinePath + ":" + lineNumber);
        }
        entries.add(entry);
      }
    } catch (IOException exception) {
      throw new LinterException("Failed reading baseline: " + baselinePath, exception);
    }

    entries.sort(Comparator.comparing(BaselineEntry::findingId));
    return List.copyOf(entries);
  }

  public Set<String> readFindingIds(Path baselinePath) throws LinterException {
    var ids = new LinkedHashSet<String>();
    for (BaselineEntry entry : readEntries(baselinePath)) {
      ids.add(entry.findingId());
    }
    return ids;
  }

  public void writeEntries(Path baselinePath, List<BaselineEntry> entries) throws LinterException {
    try {
      var parent = baselinePath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      try (BufferedWriter writer = Files.newBufferedWriter(baselinePath, StandardCharsets.UTF_8)) {
        for (BaselineEntry entry : entries) {
          writer.write(mapper.writeValueAsString(entry));
          writer.newLine();
        }
      }
    } catch (IOException exception) {
      throw new LinterException("Failed writing baseline: " + baselinePath, exception);
    }
  }

  public List<BaselineEntry> toEntries(List<Finding> findings) {
    var entries = new ArrayList<BaselineEntry>();
    var seen = new LinkedHashSet<String>();

    for (Finding finding : findings) {
      if (!seen.add(finding.findingId())) {
        continue;
      }
      entries.add(new BaselineEntry(finding.findingId(), finding.ruleId(), null, null, null));
    }

    entries.sort(Comparator.comparing(BaselineEntry::findingId));
    return List.copyOf(entries);
  }

  private BaselineEntry parseEntry(String jsonlLine, Path baselinePath, int lineNumber)
      throws LinterException {
    try {
      return mapper.readValue(jsonlLine, BaselineEntry.class);
    } catch (IOException exception) {
      throw new LinterException(
          "Invalid baseline JSONL at " + baselinePath + ":" + lineNumber, exception);
    }
  }
}
