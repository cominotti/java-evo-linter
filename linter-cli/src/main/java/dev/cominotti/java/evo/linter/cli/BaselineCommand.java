// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.cli;

import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
    name = "baseline",
    description = "Baseline management commands",
    mixinStandardHelpOptions = true,
    subcommands = {BaselineGenerateCommand.class, BaselineDiffCommand.class})
public final class BaselineCommand implements Runnable {
  @Spec private @Nullable CommandSpec spec;

  @Override
  public void run() {
    var commandSpec = commandSpec();
    commandSpec.commandLine().usage(commandSpec.commandLine().getOut());
  }

  private CommandSpec commandSpec() {
    if (spec == null) {
      throw new IllegalStateException("Command spec is unavailable before picocli initialization");
    }
    return spec;
  }
}
