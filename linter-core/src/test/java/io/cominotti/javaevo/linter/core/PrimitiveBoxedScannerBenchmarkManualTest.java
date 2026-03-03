// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.io.TempDir;

class PrimitiveBoxedScannerBenchmarkManualTest {
  @TempDir Path tempDir;

  @Test
  void scansLargeSyntheticTree(TestReporter testReporter) throws Exception {
    Assumptions.assumeTrue(
        Boolean.getBoolean("javaevo.run.benchmarks"), "Enable with -Djavaevo.run.benchmarks=true");

    var fileCount = Integer.getInteger("javaevo.benchmark.files", 2000);
    var maxMillis = Integer.getInteger("javaevo.benchmark.maxMillis", -1);

    var sourceRoot = tempDir.resolve("src/main/java/com/acme/bench");
    Files.createDirectories(sourceRoot);
    for (var index = 0; index < fileCount; index++) {
      writeCompilationUnit(sourceRoot, index);
    }

    var config = new LinterConfig().withBaseline(new BaselineSettings(Boolean.FALSE, null));

    var engine = new LinterEngine();
    engine.check(tempDir, config); // Warm up compiler and scanner paths.

    var startedAt = System.nanoTime();
    var report = engine.check(tempDir, config);
    var elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;

    var expectedFindings = fileCount * 3;
    Assertions.assertThat(report.newFindings()).hasSize(expectedFindings);
    testReporter.publishEntry(
        "benchmark",
        String.format(
            Locale.ROOT,
            "Large-tree benchmark: files=%d findings=%d elapsedMs=%d",
            fileCount,
            expectedFindings,
            elapsedMillis));

    if (maxMillis >= 0) {
      Assertions.assertThat(elapsedMillis).isLessThanOrEqualTo((long) maxMillis);
    }
  }

  private void writeCompilationUnit(Path sourceRoot, int index) throws Exception {
    var sourceFile = sourceRoot.resolve("Type" + index + ".java");
    var source =
        """
        package com.acme.bench;

        class Type%d {
            int primitive;

            String value(Integer count) {
                return "ok";
            }
        }
        """
            .formatted(index);
    Files.writeString(sourceFile, source);
  }
}
