---
paths:
  - "**/*.java"
  - "**/*.sh"
  - "linter-cli/src/main/dist/bin/java-evo-linter"
---

# Licensing

This project is licensed under the Apache License, Version 2.0 (Apache-2.0). See the
[LICENSE](../../LICENSE) file for full terms.

## SPDX Headers

All Java and shell source files must contain SPDX headers for Apache-2.0.

### Java Header Format

Java files must start with this exact first line:

```java
// SPDX-License-Identifier: Apache-2.0
```

A blank line must follow before package docs/code.

### Shell Header Format

Shell files must include:

```bash
# SPDX-License-Identifier: Apache-2.0
```

Placement rules:

1. If there is a shebang (`#!/usr/bin/env ...`), SPDX must be on line 2.
2. If there is no shebang, SPDX must be on line 1.
3. Keep one blank line after the SPDX header.

## Guardrail Commands

```bash
mvn -B -ntp validate
mvn -B -ntp -Plicense-fix validate
```

- `mvn validate`: fails when required SPDX headers are missing.
- `mvn -Plicense-fix validate`: applies missing SPDX headers to Java and shell source files.

Optional compatibility wrappers:

```bash
make license-check
make license-fix
```

## CI Enforcement

The CI workflow must run `mvn -B -ntp validate`.
