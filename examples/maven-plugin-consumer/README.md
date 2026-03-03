# Maven Plugin Consumer Example

This example demonstrates how to run `linter-maven-plugin` in a normal Maven project.

The sample project compiles with `maven.compiler.release=21`.

## Run

From repository root:

```bash
mvn -B -ntp -pl linter-core,linter-cli,linter-maven-plugin -am install
mvn -B -ntp -f examples/maven-plugin-consumer/pom.xml verify
```

The second command executes `linter-maven-plugin:check` during `verify`.
