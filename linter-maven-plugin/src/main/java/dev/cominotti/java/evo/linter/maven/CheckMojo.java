// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.maven;

import dev.cominotti.java.evo.linter.core.CheckReport;
import dev.cominotti.java.evo.linter.core.LinterEngine;
import dev.cominotti.java.evo.linter.core.LinterException;
import java.io.File;
import java.nio.file.Path;
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

    getLog().info("\n" + engine.renderHumanReport(report));

    try {
      Path effectiveJsonlPath =
          jsonlPath == null
              ? resolveProjectPath(Path.of(effectiveConfig.output().jsonlPath()))
              : resolveProjectPath(jsonlPath.toPath());
      engine.writeJsonlReport(effectiveJsonlPath, report.newFindings());
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
