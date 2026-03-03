// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BaselineFlowTest {
  @TempDir Path tempDir;

  @Test
  void supportsBaselineGenerateCheckAndDiff() throws Exception {
    writeSource(baseSource(false));

    var config =
        new LinterConfig()
            .withBaseline(
                new BaselineSettings(
                    Boolean.TRUE, tempDir.resolve(".java-evo-linter-baseline.jsonl").toString()));

    LinterEngine engine = new LinterEngine();

    BaselineGenerationReport generationReport = engine.generateBaseline(tempDir, config);
    Assertions.assertThat(generationReport.entries()).hasSize(5);
    Assertions.assertThat(Files.exists(generationReport.baselinePath())).isTrue();

    CheckReport cleanCheck = engine.check(tempDir, config);
    Assertions.assertThat(cleanCheck.newFindings()).isEmpty();
    Assertions.assertThat(cleanCheck.baselineSuppressedCount()).isEqualTo(5);

    writeSource(baseSource(true));

    CheckReport checkWithNewFinding = engine.check(tempDir, config);
    Assertions.assertThat(checkWithNewFinding.newFindings()).hasSize(1);
    Assertions.assertThat(checkWithNewFinding.staleBaselineEntries()).isEmpty();

    BaselineDiffReport diffReport = engine.diffBaseline(tempDir, config);
    Assertions.assertThat(diffReport.newFindings()).hasSize(1);
    Assertions.assertThat(diffReport.staleEntries()).isEmpty();
  }

  private void writeSource(String source) throws IOException {
    Path file = tempDir.resolve("src/main/java/com/acme/Example.java");
    Files.createDirectories(file.getParent());
    Files.writeString(file, source);
  }

  private String baseSource(boolean includeNewPrimitiveReturn) {
    String extra =
        includeNewPrimitiveReturn
            ? """

                        public int newlyAdded() {
                            return 1;
                        }
                    """
            : "";

    return """
                package com.acme;

                import java.util.List;

                public class Example {
                    private int primitiveField;
                    Integer boxedField;

                    public String name() {
                        return "x";
                    }

                    void setValues(Integer amount, List<String> names) {
                    }

                    @SuppressWarnings("primitive-boxed-signature")
                    private void suppressed(Long id) {
                    }
                %s
                }
                """
        .formatted(extra);
  }
}
