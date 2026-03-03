// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public final class LinterEngine {
  private final BaselineStore baselineStore;

  public LinterEngine() {
    this(new BaselineStore());
  }

  LinterEngine(BaselineStore baselineStore) {
    this.baselineStore = baselineStore;
  }

  public CheckReport check(Path projectRoot, LinterConfig config) throws LinterException {
    var scanner = new PrimitiveBoxedSignatureScanner(projectRoot, config);
    var scanReport = scanner.scan();

    var activeFindings = new ArrayList<Finding>(scanReport.activeFindings());
    activeFindings.sort(Finding.ORDERING);

    var baselineEnabled = Boolean.TRUE.equals(config.baseline().enabled());
    List<BaselineEntry> baselineEntries =
        baselineEnabled
            ? baselineStore.readEntries(resolveBaselinePath(projectRoot, config))
            : List.of();
    var baselineIds = new HashSet<String>();
    for (BaselineEntry baselineEntry : baselineEntries) {
      baselineIds.add(baselineEntry.findingId());
    }

    var newFindings = new ArrayList<Finding>();
    for (Finding finding : activeFindings) {
      if (!baselineIds.contains(finding.findingId())) {
        newFindings.add(finding);
      }
    }

    var activeIds = new HashSet<String>();
    for (Finding finding : activeFindings) {
      activeIds.add(finding.findingId());
    }

    var staleEntries = new ArrayList<BaselineEntry>();
    for (BaselineEntry baselineEntry : baselineEntries) {
      if (!activeIds.contains(baselineEntry.findingId())) {
        staleEntries.add(baselineEntry);
      }
    }
    staleEntries.sort(Comparator.comparing(BaselineEntry::findingId));

    var baselineSuppressedCount = activeFindings.size() - newFindings.size();
    return new CheckReport(
        List.copyOf(newFindings),
        List.copyOf(activeFindings),
        scanReport.rawFindingCount(),
        scanReport.inlineSuppressedCount(),
        baselineSuppressedCount,
        List.copyOf(staleEntries));
  }

  public BaselineGenerationReport generateBaseline(Path projectRoot, LinterConfig config)
      throws LinterException {
    var scanner = new PrimitiveBoxedSignatureScanner(projectRoot, config);
    var scanReport = scanner.scan();

    var activeFindings = new ArrayList<Finding>(scanReport.activeFindings());
    activeFindings.sort(Finding.ORDERING);
    var entries = baselineStore.toEntries(activeFindings);

    var baselinePath = resolveBaselinePath(projectRoot, config);
    baselineStore.writeEntries(baselinePath, entries);

    return new BaselineGenerationReport(
        baselinePath, entries, activeFindings.size(), scanReport.inlineSuppressedCount());
  }

  public BaselineDiffReport diffBaseline(Path projectRoot, LinterConfig config)
      throws LinterException {
    var scanner = new PrimitiveBoxedSignatureScanner(projectRoot, config);
    var scanReport = scanner.scan();

    var activeFindings = new ArrayList<Finding>(scanReport.activeFindings());
    activeFindings.sort(Finding.ORDERING);

    var baselineEntries = baselineStore.readEntries(resolveBaselinePath(projectRoot, config));
    var baselineIds = new HashSet<String>();
    for (BaselineEntry baselineEntry : baselineEntries) {
      baselineIds.add(baselineEntry.findingId());
    }

    var newFindings = new ArrayList<Finding>();
    for (Finding finding : activeFindings) {
      if (!baselineIds.contains(finding.findingId())) {
        newFindings.add(finding);
      }
    }

    var activeIds = new HashSet<String>();
    for (Finding finding : activeFindings) {
      activeIds.add(finding.findingId());
    }

    var staleEntries = new ArrayList<BaselineEntry>();
    for (BaselineEntry baselineEntry : baselineEntries) {
      if (!activeIds.contains(baselineEntry.findingId())) {
        staleEntries.add(baselineEntry);
      }
    }
    staleEntries.sort(Comparator.comparing(BaselineEntry::findingId));

    return new BaselineDiffReport(
        List.copyOf(newFindings), List.copyOf(staleEntries), activeFindings.size());
  }

  private Path resolveBaselinePath(Path projectRoot, LinterConfig config) {
    return LinterConfigLoader.normalizePath(projectRoot, Path.of(config.baseline().path()));
  }
}
