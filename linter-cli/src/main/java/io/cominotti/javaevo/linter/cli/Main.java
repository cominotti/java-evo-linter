// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.cli;

import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
    name = "java-evo-linter",
    description = "Primitive/boxed signature linter for Java production code",
    mixinStandardHelpOptions = true,
    version = "java-evo-linter 0.1.0-SNAPSHOT",
    subcommands = {CheckCommand.class, BaselineCommand.class})
public final class Main implements Runnable {
  @Spec private @Nullable CommandSpec spec;

  public static void main(String[] args) {
    var exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }

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
