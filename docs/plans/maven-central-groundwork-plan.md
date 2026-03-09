# Maven Central groundwork for `java-evo-linter`

## Repository status

Implemented in the repository:

- `groupId=dev.cominotti.java.evo` across the reactor, examples, and docs
- package moves to `dev.cominotti.java.evo.linter.*`
- Java 21 baseline in the reactor, with CI coverage on JDK 21 and JDK 25
- Central metadata plus a `central-release` Maven profile for sources, Javadocs,
  GPG signing, and Sonatype Central publishing
- tag-driven release automation in `.github/workflows/release.yml`
- example consumers for both the Maven plugin and the embeddable library
- publishing runbook in `docs/release/maven-central.md`

## Summary

- Maven Central is the right distribution channel for the embeddable library and the Maven plugin.
  Publish `linter-core`, `linter-maven-plugin`, and the `linter-cli` jar there, but keep the ZIP
  and TAR CLI bundles on GitHub Releases as the primary human-facing CLI install path.
- Adopt the suite root `dev.cominotti.java.evo`. Publish artifacts under
  `groupId=dev.cominotti.java.evo` and move code to `dev.cominotti.java.evo.linter.*`. Do not use
  `dev.cominotti.java.evo_linter`.
- Make the first Central-ready release Java 21 compatible. The current Java 25-only setup is not
  architectural; the repo currently misses Java 21 only because of two Java 22 unnamed-variable
  lambdas and the release setting.

## Key Changes

### Coordinates and naming

- `dev.cominotti.java.evo:java-evo-linter-parent`
- `dev.cominotti.java.evo:linter-core`
- `dev.cominotti.java.evo:linter-maven-plugin`
- `dev.cominotti.java.evo:linter-cli`
- Keep the existing user-facing names `java-evo-linter`, `.java-evo-linter.toml`, rule IDs, and
  Maven property prefix `javaEvo.*`.

### Package and API hardening

- Rename module packages to `dev.cominotti.java.evo.linter.core`,
  `dev.cominotti.java.evo.linter.cli`, and `dev.cominotti.java.evo.linter.maven`.
- Before the first public release, explicitly shrink the public `linter-core` surface: keep
  `LinterEngine`, config and report model types, enums, and `LinterException` public; move
  scanner, hash, path, and reporter helpers into package-private code or `...core.internal`.
- Add `Automatic-Module-Name` for `linter-core` and package and class Javadocs for the supported
  embeddable API.

### Java 21 compatibility and CI

- Change the compiler and release baseline from 25 to 21 across the reactor, docs, and examples.
- Replace the two `_` lambda parameters that currently block `--release 21` compilation and rerun
  all quality gates at the new baseline.
- Make JDK 21 the primary CI and release JDK and keep one compatibility job on JDK 25.

### Central publishing setup

- Claim the Sonatype namespace for `cominotti.dev` via DNS TXT verification, then publish under
  `dev.cominotti.java.evo`.
- In the root `pom.xml`, add the current Central Portal release path: complete POM metadata
  (`name`, `description`, `url`, `licenses`, `developers`, `scm`), `maven-source-plugin`,
  `maven-javadoc-plugin`, `maven-gpg-plugin`, and Sonatype's
  `central-publishing-maven-plugin`.
- Do not use the legacy OSSRH or `nexus-staging-maven-plugin` path.
- Add a tag-driven GitHub Actions release workflow that runs full verification, imports the GPG
  key, publishes the signed bundle to Central, and attaches CLI archives to the GitHub Release.

### Consumer docs and examples

- Update `README.md` and the example consumer to use published Central coordinates instead of
  reactor-local installs.
- Add one embeddable-library example that imports `linter-core` and runs `LinterEngine`
  programmatically.
- Add a short publishing runbook covering namespace setup, required secrets, release command,
  rollback expectations, and the fact that Central is for immutable releases while CLI archives
  stay on GitHub Releases.
- Use the current official Sonatype docs as the release contract:
  `https://central.sonatype.org/publish/requirements/`,
  `https://central.sonatype.org/register-a-namespace/`, and
  `https://central.sonatype.com/publishing/deployments`.

## Test Plan

- `mvn -B -ntp verify` passes on JDK 21 for the full reactor.
- A second CI job proves the same reactor still passes on JDK 25.
- `examples/maven-plugin-consumer` verifies on JDK 21 using
  `dev.cominotti.java.evo:linter-maven-plugin` from published or locally installed release
  coordinates.
- A small library-consumer smoke test imports `linter-core` from release coordinates and
  successfully runs `LinterEngine`.
- Release CI produces signed main jars, `-sources.jar`, and `-javadoc.jar` for every published
  artifact, and Central Portal accepts the deployment bundle without metadata or signing errors.

## Assumptions

- There are no external consumers that require compatibility with `dev.cominotti.java.evo.*`, so
  this is a clean pre-publication break.
- You can add the required Sonatype TXT record for `cominotti.dev`.
- Gradle plugin support, snapshot distribution, and non-Maven build-tool integrations are out of
  scope for this groundwork.
