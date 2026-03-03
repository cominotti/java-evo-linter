package io.cominotti.javaevo.linter.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PrimitiveBoxedScannerBenchmarkManual {
  @TempDir Path tempDir;

  @Test
  void scansLargeSyntheticTree() throws Exception {
    Assumptions.assumeTrue(
        Boolean.getBoolean("javaevo.run.benchmarks"), "Enable with -Djavaevo.run.benchmarks=true");

    var fileCount = Integer.getInteger("javaevo.benchmark.files", 2000);
    var maxMillis = Integer.getInteger("javaevo.benchmark.maxMillis", -1);

    var sourceRoot = tempDir.resolve("src/main/java/com/acme/bench");
    Files.createDirectories(sourceRoot);
    for (var index = 0; index < fileCount; index++) {
      writeCompilationUnit(sourceRoot, index);
    }

    var config = new LinterConfig();
    config.baseline.enabled = false;

    var engine = new LinterEngine();
    engine.check(tempDir, config); // Warm up compiler and scanner paths.

    var startedAt = System.nanoTime();
    var report = engine.check(tempDir, config);
    var elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;

    var expectedFindings = fileCount * 3;
    assertThat(report.newFindings()).hasSize(expectedFindings);

    System.out.printf(
        Locale.ROOT,
        "Large-tree benchmark: files=%d findings=%d elapsedMs=%d%n",
        fileCount,
        expectedFindings,
        elapsedMillis);

    if (maxMillis >= 0) {
      assertThat(elapsedMillis).isLessThanOrEqualTo((long) maxMillis);
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
