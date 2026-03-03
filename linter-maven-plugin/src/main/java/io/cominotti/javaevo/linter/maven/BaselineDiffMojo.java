// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.maven;

import io.cominotti.javaevo.linter.core.BaselineDiffReport;
import io.cominotti.javaevo.linter.core.HumanReporter;
import io.cominotti.javaevo.linter.core.LinterEngine;
import io.cominotti.javaevo.linter.core.LinterException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
    name = "baseline-diff",
    defaultPhase = LifecyclePhase.NONE,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public final class BaselineDiffMojo extends AbstractLinterMojo {
  @Parameter(property = "javaEvo.failOnMissing", defaultValue = "true")
  boolean failOnMissing;

  @Parameter(property = "javaEvo.failOnStale", defaultValue = "false")
  boolean failOnStale;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    var config = loadEffectiveConfig();
    var engine = new LinterEngine();

    BaselineDiffReport report;
    try {
      report = engine.diffBaseline(projectRoot(), config);
    } catch (LinterException exception) {
      throw new MojoExecutionException(exception.getMessage(), exception);
    }

    var humanReporter = new HumanReporter();
    getLog().info("\n" + humanReporter.renderBaselineDiffReport(report));

    if (failOnMissing && !report.newFindings().isEmpty()) {
      throw new MojoFailureException(
          "java-evo-linter baseline is missing " + report.newFindings().size() + " findings");
    }
    if (failOnStale && !report.staleEntries().isEmpty()) {
      throw new MojoFailureException(
          "java-evo-linter baseline has " + report.staleEntries().size() + " stale entries");
    }
  }
}
