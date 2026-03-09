// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.cli;

import dev.cominotti.java.evo.linter.core.OutputFormat;
import picocli.CommandLine;

public final class OutputFormatConverter implements CommandLine.ITypeConverter<OutputFormat> {
  @Override
  public OutputFormat convert(String value) {
    return OutputFormat.fromString(value);
  }
}
