package io.cominotti.javaevo.linter.maven;

import io.cominotti.javaevo.linter.core.LinterConfig;
import io.cominotti.javaevo.linter.core.LinterConfigLoader;
import io.cominotti.javaevo.linter.core.LinterConfigOverrides;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jspecify.annotations.Nullable;

abstract class AbstractLinterMojo extends AbstractMojo {
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  @Nullable MavenProject project;

  @Parameter(property = "javaEvo.config", defaultValue = "${project.basedir}/.java-evo-linter.toml")
  @Nullable File configFile;

  @Parameter(
      property = "javaEvo.baseline",
      defaultValue = "${project.basedir}/.java-evo-linter-baseline.jsonl")
  @Nullable File baselineFile;

  @Parameter(property = "javaEvo.disablePrivate", defaultValue = "false")
  boolean disablePrivate;

  @Parameter(property = "javaEvo.disablePackagePrivate", defaultValue = "false")
  boolean disablePackagePrivate;

  @Parameter(property = "javaEvo.disablePrivateFields", defaultValue = "false")
  boolean disablePrivateFields;

  @Parameter(property = "javaEvo.disablePackagePrivateFields", defaultValue = "false")
  boolean disablePackagePrivateFields;

  @Parameter(property = "javaEvo.disablePrivateMethods", defaultValue = "false")
  boolean disablePrivateMethods;

  @Parameter(property = "javaEvo.disablePackagePrivateMethods", defaultValue = "false")
  boolean disablePackagePrivateMethods;

  private MavenProject requireProject() {
    if (project == null) {
      throw new IllegalStateException("Maven project was not injected");
    }
    return project;
  }

  protected Path projectRoot() {
    return requireProject().getBasedir().toPath().toAbsolutePath().normalize();
  }

  protected LinterConfig loadEffectiveConfig() throws MojoExecutionException {
    try {
      var mavenProject = requireProject();
      var root = projectRoot();
      Path configPath = configFile != null && configFile.exists() ? configFile.toPath() : null;

      var loader = new LinterConfigLoader();
      var base = loader.load(configPath, root);

      var overrides = new LinterConfigOverrides();
      overrides.baselinePath = baselineFile == null ? null : baselineFile.toPath();
      overrides.sourceRoots = toPathList(mavenProject.getCompileSourceRoots());
      overrides.classpathEntries =
          mavenProject.getCompileClasspathElements().stream().map(Path::of).toList();

      if (disablePrivate || disablePrivateFields) {
        overrides.includePrivateFields = false;
      }
      if (disablePackagePrivate || disablePackagePrivateFields) {
        overrides.includePackagePrivateFields = false;
      }
      if (disablePrivate || disablePrivateMethods) {
        overrides.includePrivateMethods = false;
      }
      if (disablePackagePrivate || disablePackagePrivateMethods) {
        overrides.includePackagePrivateMethods = false;
      }

      return loader.applyOverrides(base, overrides, root);
    } catch (Exception exception) {
      throw new MojoExecutionException("Failed building linter configuration", exception);
    }
  }

  private List<Path> toPathList(List<String> rawPaths) {
    var paths = new ArrayList<Path>();
    for (String rawPath : rawPaths) {
      paths.add(Path.of(rawPath));
    }
    return paths;
  }
}
