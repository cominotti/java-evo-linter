# java-evo-linter Agent Precepts

These rules are the default operating contract for agents working in this repository.

## Rules and Skills Index Sync Map

This index is mandatory and must always be kept up to date when any file under `.agents/rules` or
`.agents/skills` is added, updated, moved, or removed.

Sync directives:

- Update this index in the same change that modifies rules/skills files.
- Keep hyperlinks valid and pointing to the current file locations.
- Add new entries for new files and remove entries for deleted files.
- If a file is renamed or moved, update both its path and description here.

### Rules Files (`.agents/rules`)

| File | Purpose |
| --- | --- |
| [`git-commits.md`](./rules/git-commits.md) | Commit/merge policy (signed commits, squash merge strategy, commit message format). |
| [`licensing.md`](./rules/licensing.md) | Apache-2.0 and SPDX header requirements, with Maven-first license guardrails. |

### Skills Files (`.agents/skills`)

| File | Purpose |
| --- | --- |
| [`java-coding/SKILL.md`](./skills/java-coding/SKILL.md) | Mandatory Java coding skill and Java-specific workflow/quality guidance. |
| [`learn/SKILL.md`](./skills/learn/SKILL.md) | Post-change learning workflow to keep agent docs, rules, skills, and memory artifacts current. |

## Mission

- Keep `java-evo-linter` reliable, deterministic, and strict by default.
- Prefer clear, test-backed changes over speculative or stylistic refactors.
- Preserve compatibility goals: build and runtime on Java 21+, with an explicit compatibility lane
  on JDK 25.

## Baseline and Compatibility

- Treat Java 21 as the project build and runtime baseline.
- Keep the CI compatibility lane on JDK 25 green after baseline changes.
- Keep consumer compatibility for projects that compile with `maven.compiler.release=21`.
- Assume host JDK compiler level is used during linting unless explicitly changed by project owners.

## Required Quality Gates

- `mvn -B -ntp verify` is the primary pre-merge gate and must pass.
- Keep all configured linters strict and enabled:
  - Checkstyle
  - PMD (including CPD)
  - SpotBugs
  - Error Prone + NullAway
- Keep OpenRewrite `var` policy active and enforceable via `rewrite:dryRun`.
- SonarCloud branch policy:
  - Prefer SonarCloud Automatic Analysis for PR/branch scans.
  - If Automatic Analysis is enabled, do not treat manual `make sonar-local` runs as canonical.
  - For branch remediation, query Sonar issues by PR scope (`pullRequest=<id>`) to avoid stale
    `main` findings.

## Configuration and Policy Contracts

- Configuration format is TOML only (`.toml`). Do not reintroduce YAML support.
- Preserve default annotation-based exclusions for owner types:
  - `fieldLikeOwnerAnnotations = ["EvoType"]`
  - `parameterOwnerAnnotations = ["EvoType"]`
- Keep annotation matching name-based (simple/FQCN), without requiring a dependency on `java-evo`.
- Keep suppression and baseline behavior stable and deterministic (`finding_id`-based).
- Keep licensing guardrails active:
  - root `LICENSE` must remain Apache-2.0,
  - Java/shell source SPDX headers must use `Apache-2.0`,
  - Maven `validate` must enforce SPDX policy in CI and local workflows.
- Keep release metadata Central-ready:
  - published coordinates stay under `dev.cominotti.java.evo`,
  - `linter-core` keeps `Automatic-Module-Name=dev.cominotti.java.evo.linter.core`,
  - tag releases publish through the `central-release` Maven profile and GitHub Actions release
    workflow.
- Keep README detection examples complete: include at least one example per emitted
  `violation_role` condition (`field_type`, `method_return_type`, `method_parameter_type`,
  `record_component_type`).
- Treat `linter-core/src/main/java/io/cominotti/javaevo/linter/core/ViolationRole.java` as the
  canonical condition list for README condition coverage.
- Keep README `@EvoType` coverage complete: include one fully valid annotated Java
  class example and one fully valid annotated Record example (both 0 findings under default
  exclusions).
- Keep README examples structured by declaration shape:
  - separate `Classes` and `Records` sections,
  - per-rule subsections with associated flags/config settings,
  - one valid and one invalid example per rule,
  - explicit and descriptive validity/violation messages for each example.

## Change Hygiene

- When changing behavior, update:
  - docs (`README.md`, example docs/config when relevant),
  - tests (unit + integration semantics),
  - CI/workflow assumptions if runtime/tooling baseline changes.
- When changing release or distribution behavior, update:
  - `docs/release/maven-central.md`,
  - `scripts/release/*.sh`,
  - `.github/workflows/release.yml`,
  - example coordinates and README installation snippets.
- If emitted `violation_role` conditions or detection surfaces change, update README condition
  coverage examples in the same change.
- If `@EvoType` exclusion behavior changes, update both fully valid shape examples
  (class and Record), plus all related per-rule valid/invalid examples and messages in README in
  the same change.
- Validate plugin usability through the example consumer flow when plugin or core scanner behavior changes.
- Validate the embeddable API through `examples/library-consumer` when the supported `linter-core`
  surface changes.

## Verification Checklist

Run these whenever relevant:

1. `mvn -B -ntp validate`
2. `mvn -B -ntp verify`
3. `mvn -B -ntp rewrite:dryRun`
4. `mvn -B -ntp -pl linter-core,linter-cli,linter-maven-plugin -am install`
5. `mvn -B -ntp -f examples/maven-plugin-consumer/pom.xml verify`
6. `mvn -B -ntp -f examples/library-consumer/pom.xml test`
7. `mvn -B -ntp -Pcentral-release -Dgpg.skip=true verify`
8. `make central-preflight`
