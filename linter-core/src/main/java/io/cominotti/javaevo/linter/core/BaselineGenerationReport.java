// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import java.nio.file.Path;
import java.util.List;

public record BaselineGenerationReport(
    Path baselinePath,
    List<BaselineEntry> entries,
    int activeFindingCount,
    int inlineSuppressedCount) {}
