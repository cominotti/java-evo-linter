package io.cominotti.javaevo.linter.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HumanReporter {
  public String renderCheckReport(CheckReport report) {
    var output = new StringBuilder();

    renderFindings("New findings", report.newFindings(), output);

    output.append("\nSummary\n");
    output.append("  Raw findings: ").append(report.rawFindingCount()).append('\n');
    output.append("  Inline suppressed: ").append(report.inlineSuppressedCount()).append('\n');
    output.append("  Baseline suppressed: ").append(report.baselineSuppressedCount()).append('\n');
    output.append("  New findings: ").append(report.newFindings().size()).append('\n');
    output.append("  Active findings: ").append(report.activeFindings().size()).append('\n');
    output
        .append("  Stale baseline entries: ")
        .append(report.staleBaselineEntries().size())
        .append('\n');

    if (!report.staleBaselineEntries().isEmpty()) {
      output.append("\nStale baseline entries\n");
      for (BaselineEntry staleEntry : report.staleBaselineEntries()) {
        output.append("  - ").append(staleEntry.findingId()).append('\n');
      }
    }

    return output.toString().trim();
  }

  public String renderBaselineDiffReport(BaselineDiffReport report) {
    var output = new StringBuilder();
    renderFindings("Findings missing in baseline", report.newFindings(), output);

    output.append("\nStale baseline entries\n");
    if (report.staleEntries().isEmpty()) {
      output.append("  None\n");
    } else {
      for (BaselineEntry staleEntry : report.staleEntries()) {
        output.append("  - ").append(staleEntry.findingId()).append('\n');
      }
    }

    output.append("\nSummary\n");
    output.append("  Active findings: ").append(report.activeFindingCount()).append('\n');
    output.append("  Missing in baseline: ").append(report.newFindings().size()).append('\n');
    output.append("  Stale baseline entries: ").append(report.staleEntries().size()).append('\n');

    return output.toString().trim();
  }

  private void renderFindings(String title, List<Finding> findings, StringBuilder output) {
    output.append(title).append('\n');
    if (findings.isEmpty()) {
      output.append("  None\n");
      return;
    }

    var byFile = new LinkedHashMap<String, List<Finding>>();
    for (Finding finding : findings) {
      byFile.computeIfAbsent(finding.file(), _ -> new ArrayList<>()).add(finding);
    }

    for (Map.Entry<String, List<Finding>> fileEntry : byFile.entrySet()) {
      output.append("\n").append(fileEntry.getKey()).append('\n');
      for (Finding finding : fileEntry.getValue()) {
        output
            .append("  ")
            .append(finding.line())
            .append(':')
            .append(finding.column())
            .append(" [")
            .append(finding.ruleId())
            .append("] ")
            .append(finding.violationRole())
            .append(" -> ")
            .append(finding.forbiddenType())
            .append(" in ")
            .append(finding.memberSignature())
            .append('\n');
        output
            .append("    id=")
            .append(finding.findingId())
            .append(" declared=")
            .append(finding.declaredType())
            .append('\n');
      }
    }
  }
}
