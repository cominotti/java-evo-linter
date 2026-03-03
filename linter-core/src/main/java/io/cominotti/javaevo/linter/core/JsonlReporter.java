// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class JsonlReporter {
  private final ObjectMapper objectMapper = new ObjectMapper();

  public void writeToPath(Path outputPath, List<Finding> findings) throws LinterException {
    try {
      var parent = outputPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
        writeToWriter(writer, findings);
      }
    } catch (IOException exception) {
      throw new LinterException("Failed writing JSONL report: " + outputPath, exception);
    }
  }

  public void writeToStream(OutputStream outputStream, List<Finding> findings)
      throws LinterException {
    try (var writer =
        new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
      writeToWriter(writer, findings);
      writer.flush();
    } catch (IOException exception) {
      throw new LinterException("Failed writing JSONL report to stream", exception);
    }
  }

  private void writeToWriter(BufferedWriter writer, List<Finding> findings) throws IOException {
    for (Finding finding : findings) {
      writer.write(objectMapper.writeValueAsString(finding));
      writer.newLine();
    }
  }
}
