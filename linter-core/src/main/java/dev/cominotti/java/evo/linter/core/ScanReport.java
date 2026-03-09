// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

import java.util.List;

record ScanReport(List<Finding> activeFindings, int rawFindingCount, int inlineSuppressedCount) {}
