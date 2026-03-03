// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import java.util.List;

public record CheckReport(
    List<Finding> newFindings,
    List<Finding> activeFindings,
    int rawFindingCount,
    int inlineSuppressedCount,
    int baselineSuppressedCount,
    List<BaselineEntry> staleBaselineEntries) {}
