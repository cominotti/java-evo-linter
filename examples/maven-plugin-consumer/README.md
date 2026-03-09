# Maven Plugin Consumer Example

This example demonstrates how to run `linter-maven-plugin` from the
`dev.cominotti.java.evo:linter-maven-plugin` coordinates in a normal Maven project.

The sample project compiles with `maven.compiler.release=21`.

## Run

From repository root:

```bash
mvn -B -ntp -pl linter-core,linter-cli,linter-maven-plugin -am install
mvn -B -ntp -f examples/maven-plugin-consumer/pom.xml verify
```

The example uses the Central-style coordinates with `java.evo.linter.version=0.1.0-SNAPSHOT`.
During local repo verification, the first command installs that snapshot into `~/.m2`.
