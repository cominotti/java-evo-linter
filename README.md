# java-evo-linter

`java-evo-linter` detects primitive and boxed/JDK scalar types used in production class fields and method/constructor signatures.

## Compatibility

- Runtime: Java 25+
- Compiled with `--release 25`
- Linting uses the host JDK compiler level
- Java 21 projects are supported (for example, Maven projects with `maven.compiler.release=21` running on JDK 25)
- Maven plugin and CLI are both available

## CI

GitHub Actions workflow: `.github/workflows/ci.yml`

- Build and test on Temurin JDK 25
- Builds and uploads CLI distribution archives (`zip` and `tar.gz`)
- Builds a standalone Maven consumer example targeting `--release 21` while running on JDK 25

## Linting

`mvn verify` enforces these linters in strict mode:

- fmt-maven-plugin (`fmt:check`)
- Checkstyle
- PMD (including CPD)
- SpotBugs
- Error Prone + NullAway (during compilation)

### Formatter (`fmt-maven-plugin`)

`com.spotify.fmt:fmt-maven-plugin` is configured in the parent POM (Google Java Format style).

Check formatting:

```bash
mise exec -- mvn -B -ntp fmt:check
```

Apply formatting:

```bash
mise exec -- mvn -B -ntp fmt:format
```

### OpenRewrite (`var` policy)

OpenRewrite is configured with the `UseVar` recipe (`org.openrewrite.java.migrate.lang.UseVar`).

Preview required `var` changes:

```bash
mise exec -- mvn -B -ntp rewrite:dryRun
```

Apply the changes:

```bash
mise exec -- mvn -B -ntp rewrite:run
```

## Testing

Run all unit and integration-style module tests:

```bash
mise exec -- mvn -B -ntp test
```

Run full verification:

```bash
mise exec -- mvn -B -ntp verify
```

Run example consumer verification:

```bash
mise exec -- mvn -B -ntp -pl linter-core,linter-cli,linter-maven-plugin -am install
mise exec -- mvn -B -ntp -f examples/maven-plugin-consumer/pom.xml verify
```

Run the large-tree scanner benchmark (manual):

```bash
./scripts/benchmark/run-large-tree-scan.sh
```

Optional benchmark environment variables:

- `JAVA_EVO_BENCH_FILES` (default `2000`)
- `JAVA_EVO_BENCH_MAX_MILLIS` (default `-1`; when set, benchmark fails if exceeded)

## What It Detects

Rule ID: `primitive-boxed-signature`

By default it flags usage of:

- Primitives: `boolean`, `byte`, `short`, `int`, `long`, `float`, `double`, `char`
- Boxed types: `java.lang.Boolean`, `Byte`, `Short`, `Integer`, `Long`, `Float`, `Double`, `Character`
- `java.lang.String`

Detection surfaces:

- Field types
- Method return types
- Method/constructor parameter types
- Record component types
- Nested generic type arguments (for example `List<String>`)

Default owner-annotation exclusions:

- Field types, record components, and method/constructor parameters are skipped when the owner type is annotated with `@EnterpriseValueObject`
- Method return types are still checked
- Matching is name-based (no dependency on a specific annotation library)

### Examples by Declaration Shape

The examples below are intentionally split into `Classes` and `Records`. Each rule subsection
includes:

- associated config/flags that affect behavior
- one invalid example (should be reported)
- one valid example (should not be reported)
- a clear explanation for why each example is valid/invalid

#### Classes

##### Rule: `field_type` (class fields)

Associated flags/configs:

- `forbiddenTypes`
- `[visibility.fields].includePrivate` / `[visibility.fields].includePackagePrivate`
- `--disable-private-fields` / `--disable-package-private-fields`
- `[annotatedTypeExclusions].fieldLikeOwnerAnnotations` (class-level owner exclusion)

Invalid example:

```java
package com.acme;

final class ClassFieldInvalid {
    int quantity;
}
```

Invalid message: This is reported as `field_type` because `quantity` is a primitive class field.

Valid example:

```java
package com.acme;

record QuantityValue(java.math.BigDecimal amount) {}

final class ClassFieldValid {
    QuantityValue quantity;
}
```

Valid message: This is valid because the field type is a domain type (`QuantityValue`), not a forbidden primitive/boxed/string type.

##### Rule: `method_return_type` (class methods)

Associated flags/configs:

- `forbiddenTypes`
- `[visibility.methods].includePrivate` / `[visibility.methods].includePackagePrivate`
- `--disable-private-methods` / `--disable-package-private-methods`
- Owner annotation exclusions do not skip method return checks

Invalid example:

```java
package com.acme;

final class ClassReturnInvalid {
    String nextId() {
        return "legacy-id";
    }
}
```

Invalid message: This is reported as `method_return_type` because returning `String` is forbidden by default.

Valid example:

```java
package com.acme;

record InvoiceId(java.util.UUID value) {}

final class ClassReturnValid {
    InvoiceId nextId() {
        return new InvoiceId(java.util.UUID.randomUUID());
    }
}
```

Valid message: This is valid because the method returns a domain type (`InvoiceId`) instead of a forbidden scalar.

##### Rule: `method_parameter_type` (class methods/constructors)

Associated flags/configs:

- `forbiddenTypes`
- `[visibility.methods].includePrivate` / `[visibility.methods].includePackagePrivate`
- `--disable-private-methods` / `--disable-package-private-methods`
- `[annotatedTypeExclusions].parameterOwnerAnnotations` (class-level owner exclusion for parameters)

Invalid example:

```java
package com.acme;

final class ClassParameterInvalid {
    void apply(Integer percentage) {
    }
}
```

Invalid message: This is reported as `method_parameter_type` because the method parameter uses boxed `Integer`.

Valid example:

```java
package com.acme;

record DiscountRate(java.math.BigDecimal value) {}

final class ClassParameterValid {
    void apply(DiscountRate percentage) {
    }
}
```

Valid message: This is valid because the parameter is modeled as a domain type (`DiscountRate`).

##### Rule: `@EnterpriseValueObject` owner exclusions (classes)

Associated flags/configs:

- `[annotatedTypeExclusions].fieldLikeOwnerAnnotations`
- `[annotatedTypeExclusions].parameterOwnerAnnotations`
- `fieldLikeOwnerAnnotations = ["EnterpriseValueObject"]` (default)
- `parameterOwnerAnnotations = ["EnterpriseValueObject"]` (default)

Valid example:

```java
package com.acme;

@interface EnterpriseValueObject {}
record Price(java.math.BigDecimal value) {}

@EnterpriseValueObject
final class ClassOwnerExcludedValid {
    int legacyCents;

    ClassOwnerExcludedValid(Integer seed) {
    }

    Price value(Price fallback) {
        return fallback;
    }
}
```

Valid message: This is valid under default exclusions because the owner annotation suppresses class field and parameter findings; the return type is a domain type (`Price`).

Invalid example:

```java
package com.acme;

@interface EnterpriseValueObject {}

@EnterpriseValueObject
final class ClassOwnerExcludedInvalid {
    int legacyCents;

    ClassOwnerExcludedInvalid(Integer seed) {
    }

    String status() {
        return "legacy";
    }
}
```

Invalid message: This is still reported as `method_return_type` because owner annotation exclusions do not suppress method return checks.

#### Records

##### Rule: `record_component_type` (record components)

Associated flags/configs:

- `forbiddenTypes`
- `[annotatedTypeExclusions].fieldLikeOwnerAnnotations` (record owner exclusion)
- Record components remain checked even if field visibility flags disable private/package-private fields

Invalid example:

```java
package com.acme;

record RecordComponentInvalid(Integer componentId) {}
```

Invalid message: This is reported as `record_component_type` because the component type is boxed `Integer`.

Valid example:

```java
package com.acme;

record ComponentId(java.util.UUID value) {}
record RecordComponentValid(ComponentId componentId) {}
```

Valid message: This is valid because the record component uses a domain type (`ComponentId`).

##### Rule: `method_return_type` (record methods)

Associated flags/configs:

- `forbiddenTypes`
- `[visibility.methods].includePrivate` / `[visibility.methods].includePackagePrivate`
- `--disable-private-methods` / `--disable-package-private-methods`
- Owner annotation exclusions do not skip method return checks

Invalid example:

```java
package com.acme;

record RecordReturnInvalid(java.math.BigDecimal amount) {
    String summary() {
        return "legacy";
    }
}
```

Invalid message: This is reported as `method_return_type` because record methods returning `String` are forbidden by default.

Valid example:

```java
package com.acme;

record LedgerView(java.util.UUID value) {}

record RecordReturnValid(java.math.BigDecimal amount) {
    LedgerView summary() {
        return new LedgerView(java.util.UUID.randomUUID());
    }
}
```

Valid message: This is valid because the return type is a domain type (`LedgerView`).

##### Rule: `method_parameter_type` (record methods/constructors)

Associated flags/configs:

- `forbiddenTypes`
- `[visibility.methods].includePrivate` / `[visibility.methods].includePackagePrivate`
- `--disable-private-methods` / `--disable-package-private-methods`
- `[annotatedTypeExclusions].parameterOwnerAnnotations` (record owner exclusion for parameters)

Invalid example:

```java
package com.acme;

record RecordParameterInvalid(java.math.BigDecimal amount) {
    void update(Integer nextValue) {
    }
}
```

Invalid message: This is reported as `method_parameter_type` because record method parameters use the same forbidden-type rule.

Valid example:

```java
package com.acme;

record LimitValue(java.math.BigDecimal value) {}

record RecordParameterValid(LimitValue amount) {
    void update(LimitValue nextValue) {
    }
}
```

Valid message: This is valid because method/constructor parameters are domain types (`LimitValue`).

##### Rule: `field_type` (static fields declared in records)

Associated flags/configs:

- `forbiddenTypes`
- `[visibility.fields].includePrivate` / `[visibility.fields].includePackagePrivate`
- `--disable-private-fields` / `--disable-package-private-fields`
- `[annotatedTypeExclusions].fieldLikeOwnerAnnotations` (record owner exclusion)

Invalid example:

```java
package com.acme;

record RecordFieldInvalid(java.util.UUID id) {
    static int counter = 0;
}
```

Invalid message: This is reported as `field_type` because static fields in records are checked like class fields.

Valid example:

```java
package com.acme;

record CounterValue(java.math.BigDecimal value) {}

record RecordFieldValid(java.util.UUID id) {
    static CounterValue counter = new CounterValue(java.math.BigDecimal.ZERO);
}
```

Valid message: This is valid because the static field uses a domain type (`CounterValue`) instead of a forbidden scalar.

##### Rule: `@EnterpriseValueObject` owner exclusions (records)

Associated flags/configs:

- `[annotatedTypeExclusions].fieldLikeOwnerAnnotations`
- `[annotatedTypeExclusions].parameterOwnerAnnotations`
- `fieldLikeOwnerAnnotations = ["EnterpriseValueObject"]` (default)
- `parameterOwnerAnnotations = ["EnterpriseValueObject"]` (default)

Valid example:

```java
package com.acme;

@interface EnterpriseValueObject {}
record SnapshotId(java.util.UUID value) {}

@EnterpriseValueObject
record RecordOwnerExcludedValid(Integer version) {
    SnapshotId id(SnapshotId fallback) {
        return fallback;
    }
}
```

Valid message: This is valid under default exclusions because the owner annotation suppresses record-component and constructor-parameter findings; the return type is not forbidden.

Invalid example:

```java
package com.acme;

@interface EnterpriseValueObject {}

@EnterpriseValueObject
record RecordOwnerExcludedInvalid(Integer version) {
    String label() {
        return "legacy";
    }
}
```

Invalid message: This is still reported as `method_return_type` because owner annotation exclusions do not suppress record method return checks.

Only production sources are scanned by default (`src/main/java`).

## Suppression and Baselining

### Inline suppression

Use `@SuppressWarnings` with one of:

- `primitive-boxed-signature`
- `java-evo-linter:primitive-boxed-signature`

Supported at class and member level.
For records, `@SuppressWarnings` on a record component suppresses the `record_component_type` finding for
that component; constructor-parameter findings follow constructor/method suppression scope.

### Baseline suppression

Use JSONL baseline entries keyed by stable `finding_id`.

- Generate baseline: `baseline generate`
- Compare baseline drift: `baseline diff`
- In `check`, baseline entries suppress matching findings and report stale entries

## CLI Usage

Build CLI shaded jar:

```bash
mise exec -- mvn -q -pl linter-cli -am package
```

The shaded artifact merges Apache `LICENSE` and `NOTICE` metadata into explicit top-level entries.

Run checks:

```bash
java -jar linter-cli/target/linter-cli-0.1.0-SNAPSHOT.jar check
```

### Build Release Distribution

```bash
./scripts/release/build-dist.sh
```

This produces:

- `linter-cli/target/linter-cli-<version>-dist.zip`
- `linter-cli/target/linter-cli-<version>-dist.tar.gz`

The archive layout is:

- `bin/java-evo-linter` (launcher script)
- `lib/java-evo-linter-cli.jar` (self-contained CLI runtime)

Run from extracted archive:

```bash
./bin/java-evo-linter check --help
```

Common options:

```bash
java -jar linter-cli/target/linter-cli-0.1.0-SNAPSHOT.jar check \
  --config .java-evo-linter.toml \
  --baseline .java-evo-linter-baseline.jsonl \
  --format both \
  --jsonl-path build/java-evo-linter/findings.jsonl
```

Disable private or package-private globally via flags:

- `--disable-private`
- `--disable-package-private`
- `--disable-private-fields`
- `--disable-private-methods`
- `--disable-package-private-fields`
- `--disable-package-private-methods`

Baseline commands:

```bash
java -jar linter-cli/target/linter-cli-0.1.0-SNAPSHOT.jar baseline generate
java -jar linter-cli/target/linter-cli-0.1.0-SNAPSHOT.jar baseline diff --fail-on-stale
```

## Plugin Consumer Example

See [`examples/maven-plugin-consumer`](examples/maven-plugin-consumer).

From repository root:

```bash
mvn -B -ntp -pl linter-core,linter-cli,linter-maven-plugin -am install
mvn -B -ntp -f examples/maven-plugin-consumer/pom.xml verify
```

The example runs `linter-maven-plugin:check` in its `verify` phase.

## Maven Plugin Usage

Install plugin (artifact currently from this multi-module build):

```xml
<plugin>
  <groupId>io.cominotti.javaevo</groupId>
  <artifactId>linter-maven-plugin</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <executions>
    <execution>
      <goals>
        <goal>check</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

Goals:

- `check` (default bound to `verify`)
- `baseline-generate`
- `baseline-diff`

Plugin parameters include:

- `javaEvo.config`
- `javaEvo.baseline`
- `javaEvo.failOnNewFindings`
- `javaEvo.failOnMissing`
- `javaEvo.failOnStale`
- visibility disable flags (same semantics as CLI)

## Config Example

```toml
sourceRoots = ["src/main/java"]
includeGlobs = ["**/*.java"]
excludeGlobs = ["**/generated/**"]

forbiddenTypes = ["int", "long", "java.lang.Integer", "java.lang.String"]
classpath = ["build/classes/java/main"]
failOnCompileErrors = true

[visibility.fields]
includePrivate = true
includePackagePrivate = true

[visibility.methods]
includePrivate = true
includePackagePrivate = true

[[packageOverrides]]
pattern = "com.acme.internal.**"

[packageOverrides.fields]
includePrivate = false
includePackagePrivate = false

[packageOverrides.methods]
includePrivate = false
includePackagePrivate = false

[annotatedTypeExclusions]
fieldLikeOwnerAnnotations = ["EnterpriseValueObject"]
parameterOwnerAnnotations = ["EnterpriseValueObject"]

[suppression]
inlineEnabled = true
keys = ["primitive-boxed-signature", "java-evo-linter:primitive-boxed-signature"]

[baseline]
enabled = true
path = ".java-evo-linter-baseline.jsonl"

[output]
format = "both"
jsonlPath = "build/java-evo-linter/findings.jsonl"
```

## Output

### Human output

Grouped by file with exact line/column, rule id, role, forbidden type, stable finding ID, and declaration context.

### JSONL output

Each line is a finding object with deterministic fields including:

- `schema_version`
- `rule_id`
- `finding_id`
- `file`, `line`, `column`
- `package`, `owner_type`, `member_kind`, `member_signature`
- `visibility`, `violation_role`, `forbidden_type`, `declared_type`
- `message`, `suggestion`
