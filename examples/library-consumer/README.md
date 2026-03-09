# Library Consumer Example

This example demonstrates how to embed `linter-core` from the
`dev.cominotti.java.evo:linter-core` coordinates and invoke `LinterEngine`
programmatically.

## Run

From repository root:

```bash
mvn -B -ntp -pl linter-core,linter-cli,linter-maven-plugin -am install
mvn -B -ntp -f examples/library-consumer/pom.xml test
```

The example uses `java.evo.linter.version=0.1.0-SNAPSHOT`, so the install step
provides the matching artifact in the local Maven repository during repo-local
verification.
