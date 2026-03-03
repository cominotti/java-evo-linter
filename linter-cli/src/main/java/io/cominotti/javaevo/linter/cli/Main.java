package io.cominotti.javaevo.linter.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "java-evo-linter",
    description = "Primitive/boxed signature linter for Java production code",
    mixinStandardHelpOptions = true,
    version = "java-evo-linter 0.1.0-SNAPSHOT",
    subcommands = {CheckCommand.class, BaselineCommand.class})
public final class Main implements Runnable {
  public static void main(String[] args) {
    var exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public void run() {
    CommandLine.usage(this, System.out);
  }
}
