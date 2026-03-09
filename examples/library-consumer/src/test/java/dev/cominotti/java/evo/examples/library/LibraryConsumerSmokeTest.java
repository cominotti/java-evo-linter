// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.examples.library;

import dev.cominotti.java.evo.linter.core.LinterConfig;
import dev.cominotti.java.evo.linter.core.LinterEngine;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LibraryConsumerSmokeTest {
  @Test
  void runsLinterEngineAgainstReleaseCoordinates() throws Exception {
    Path projectRoot = Files.createTempDirectory("java-evo-library-consumer");
    Path sourceFile = projectRoot.resolve("src/main/java/com/acme/OrderId.java");
    Files.createDirectories(sourceFile.getParent());
    Files.writeString(
        sourceFile,
        """
        package com.acme;

        final class OrderId {
          String value;
        }
        """);

    var engine = new LinterEngine();
    var report = engine.check(projectRoot, new LinterConfig());

    assertEquals(1, report.newFindings().size());
    assertFalse(report.activeFindings().isEmpty());
    assertEquals("primitive-boxed-signature", report.newFindings().get(0).ruleId());
  }
}
