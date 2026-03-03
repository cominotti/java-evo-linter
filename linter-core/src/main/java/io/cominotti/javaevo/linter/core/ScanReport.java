// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import java.util.List;

public record ScanReport(
    List<Finding> activeFindings, int rawFindingCount, int inlineSuppressedCount) {}
