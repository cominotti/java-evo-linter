// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

import java.util.List;

/** Result of a standard check run after baseline filtering is applied. */
public record CheckReport(
    List<Finding> newFindings,
    List<Finding> activeFindings,
    int rawFindingCount,
    int inlineSuppressedCount,
    int baselineSuppressedCount,
    List<BaselineEntry> staleBaselineEntries) {}
