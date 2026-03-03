package io.cominotti.javaevo.linter.maven;

import io.cominotti.javaevo.linter.core.CheckReport;
import io.cominotti.javaevo.linter.core.HumanReporter;
import io.cominotti.javaevo.linter.core.JsonlReporter;
import io.cominotti.javaevo.linter.core.LinterEngine;
import io.cominotti.javaevo.linter.core.LinterException;
import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.jspecify.annotations.Nullable;

@Mojo(
    name = "check",
    defaultPhase = LifecyclePhase.VERIFY,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public final class CheckMojo extends AbstractLinterMojo {
  @Parameter(property = "javaEvo.failOnNewFindings", defaultValue = "true")
  boolean failOnNewFindings;

  @Parameter(
      property = "javaEvo.jsonlPath",
      defaultValue = "${project.build.directory}/java-evo-linter-findings.jsonl")
  @Nullable File jsonlPath;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    var effectiveConfig = loadEffectiveConfig();
    var engine = new LinterEngine();

    CheckReport report;
    try {
      report = engine.check(projectRoot(), effectiveConfig);
    } catch (LinterException exception) {
      throw new MojoExecutionException(exception.getMessage(), exception);
    }

    var humanReporter = new HumanReporter();
    getLog().info("\n" + humanReporter.renderCheckReport(report));

    try {
      var jsonlReporter = new JsonlReporter();
      File effectiveJsonlPath =
          jsonlPath == null
              ? projectRoot().resolve(effectiveConfig.output.jsonlPath).toFile()
              : jsonlPath;
      jsonlReporter.writeToPath(effectiveJsonlPath.toPath(), report.newFindings());
      getLog().info("JSONL report written to " + effectiveJsonlPath);
    } catch (LinterException exception) {
      throw new MojoExecutionException(exception.getMessage(), exception);
    }

    if (failOnNewFindings && !report.newFindings().isEmpty()) {
      throw new MojoFailureException(
          "java-evo-linter found " + report.newFindings().size() + " new findings");
    }
  }
}
