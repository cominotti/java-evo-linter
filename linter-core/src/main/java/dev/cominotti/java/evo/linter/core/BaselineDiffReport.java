// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

import java.util.List;

/** Comparison between the current findings and an existing baseline file. */
public record BaselineDiffReport(
    List<Finding> newFindings, List<BaselineEntry> staleEntries, int activeFindingCount) {}
