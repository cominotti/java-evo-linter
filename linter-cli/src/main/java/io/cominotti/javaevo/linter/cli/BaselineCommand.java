// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "baseline",
    description = "Baseline management commands",
    mixinStandardHelpOptions = true,
    subcommands = {BaselineGenerateCommand.class, BaselineDiffCommand.class})
public final class BaselineCommand implements Runnable {
  @Override
  public void run() {
    CommandLine.usage(this, System.out);
  }
}
