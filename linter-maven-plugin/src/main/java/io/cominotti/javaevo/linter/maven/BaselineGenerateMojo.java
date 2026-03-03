// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.maven;

import io.cominotti.javaevo.linter.core.BaselineGenerationReport;
import io.cominotti.javaevo.linter.core.LinterEngine;
import io.cominotti.javaevo.linter.core.LinterException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
    name = "baseline-generate",
    defaultPhase = LifecyclePhase.NONE,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public final class BaselineGenerateMojo extends AbstractLinterMojo {
  @Override
  public void execute() throws MojoExecutionException {
    var config = loadEffectiveConfig();
    var engine = new LinterEngine();

    BaselineGenerationReport report;
    try {
      report = engine.generateBaseline(projectRoot(), config);
    } catch (LinterException exception) {
      throw new MojoExecutionException(exception.getMessage(), exception);
    }

    getLog().info("Baseline generated at " + report.baselinePath());
    getLog().info("Entries: " + report.entries().size());
    getLog().info("Active findings: " + report.activeFindingCount());
    getLog().info("Inline suppressed: " + report.inlineSuppressedCount());
  }
}
