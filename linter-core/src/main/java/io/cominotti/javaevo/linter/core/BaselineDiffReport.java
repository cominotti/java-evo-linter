package io.cominotti.javaevo.linter.core;

import java.util.List;

public record BaselineDiffReport(
    List<Finding> newFindings, List<BaselineEntry> staleEntries, int activeFindingCount) {}
