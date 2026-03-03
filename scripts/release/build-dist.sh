#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

if command -v mvn >/dev/null 2>&1; then
  mvn -B -ntp -pl linter-cli -am clean package
elif command -v mise >/dev/null 2>&1; then
  mise exec -- mvn -B -ntp -pl linter-cli -am clean package
else
  echo "build-dist: maven is required. Install mvn or make it available via mise." >&2
  exit 2
fi

echo "Distribution archives created:"
find linter-cli/target -maxdepth 1 -type f \( -name '*-dist.zip' -o -name '*-dist.tar.gz' \) | sort
