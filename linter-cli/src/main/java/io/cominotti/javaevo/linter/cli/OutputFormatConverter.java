// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.cli;

import io.cominotti.javaevo.linter.core.OutputFormat;
import picocli.CommandLine;

public final class OutputFormatConverter implements CommandLine.ITypeConverter<OutputFormat> {
  @Override
  public OutputFormat convert(String value) {
    return OutputFormat.fromString(value);
  }
}
