// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

import java.nio.file.Path;
import java.util.List;

/** Result of writing a new baseline file from the current findings. */
public record BaselineGenerationReport(
    Path baselinePath,
    List<BaselineEntry> entries,
    int activeFindingCount,
    int inlineSuppressedCount) {}
