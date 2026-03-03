#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

FILES="${JAVA_EVO_BENCH_FILES:-2000}"
MAX_MILLIS="${JAVA_EVO_BENCH_MAX_MILLIS:--1}"

declare -a MVN_CMD=(
  mvn
  -B
  -ntp
  -pl
  linter-core
  -Dtest=PrimitiveBoxedScannerBenchmarkManual
  -Djavaevo.run.benchmarks=true
  "-Djavaevo.benchmark.files=${FILES}"
)

if [[ "${MAX_MILLIS}" != "-1" ]]; then
  MVN_CMD+=("-Djavaevo.benchmark.maxMillis=${MAX_MILLIS}")
fi

MVN_CMD+=(test)

if command -v mvn >/dev/null 2>&1; then
  "${MVN_CMD[@]}"
elif command -v mise >/dev/null 2>&1; then
  mise exec -- "${MVN_CMD[@]}"
else
  echo "benchmark: maven is required. Install mvn or make it available via mise." >&2
  exit 2
fi
