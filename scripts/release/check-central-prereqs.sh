#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

SETTINGS_FILE="${HOME}/.m2/settings-central.xml"
ALLOW_SNAPSHOT=true
REQUIRE_CREDENTIALS=false
EXPECT_TAG=""

usage() {
  cat <<'EOF'
Usage: check-central-prereqs.sh [options]

Checks the local prerequisites for a Maven Central release.

Options:
  --settings-file PATH     Settings file to inspect for the <server id="central"> entry.
                           Default: ~/.m2/settings-central.xml
  --expect-tag TAG         Validate that the project version matches a tag like v0.1.0.
  --allow-snapshot         Allow a -SNAPSHOT version during the check.
  --require-release-version Fail if the project version still ends with -SNAPSHOT.
  --require-credentials    Fail if Central credentials are not available via env or settings file.
  -h, --help               Show this help.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --settings-file)
      SETTINGS_FILE="$2"
      shift 2
      ;;
    --expect-tag)
      EXPECT_TAG="$2"
      shift 2
      ;;
    --allow-snapshot)
      ALLOW_SNAPSHOT=true
      shift
      ;;
    --require-release-version)
      ALLOW_SNAPSHOT=false
      shift
      ;;
    --require-credentials)
      REQUIRE_CREDENTIALS=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "central-preflight: unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

failures=0
warnings=0

ok() {
  printf 'OK    %s\n' "$1"
}

warn() {
  printf 'WARN  %s\n' "$1"
  warnings=$((warnings + 1))
}

fail() {
  printf 'FAIL  %s\n' "$1"
  failures=$((failures + 1))
}

xml_server_present() {
  local file="$1"
  [[ -f "$file" ]] &&
    grep -q '<id>central</id>' "$file" &&
    grep -q '<username>' "$file" &&
    grep -q '<password>' "$file"
}

project_version="$(
  sed -n '1,20p' pom.xml | sed -n 's|.*<version>\(.*\)</version>.*|\1|p' | head -n 1
)"

if [[ -z "$project_version" ]]; then
  fail "could not determine the root project version from pom.xml"
else
  ok "project version detected: ${project_version}"
fi

if [[ "$ALLOW_SNAPSHOT" == false && "$project_version" == *-SNAPSHOT ]]; then
  fail "project version is still a SNAPSHOT; set a release version before publishing"
fi

if [[ -n "$EXPECT_TAG" ]]; then
  expected_version="${EXPECT_TAG#v}"
  if [[ "$project_version" != "$expected_version" ]]; then
    fail "tag ${EXPECT_TAG} does not match pom version ${project_version}"
  else
    ok "tag ${EXPECT_TAG} matches pom version ${project_version}"
  fi
fi

if command -v java >/dev/null 2>&1; then
  java_version="$(
    java -XshowSettings:properties -version 2>&1 | sed -n 's/ *java.version = //p' | head -n 1
  )"
  java_major="${java_version%%.*}"
  if [[ "$java_major" =~ ^[0-9]+$ ]] && (( java_major >= 21 )); then
    ok "active Java runtime is ${java_version}"
  else
    fail "active Java runtime is ${java_version:-unknown}; Java 21+ is required"
  fi
else
  fail "java is not available on PATH"
fi

if command -v mvn >/dev/null 2>&1 || command -v mise >/dev/null 2>&1; then
  ok "Maven execution is available"
else
  fail "neither mvn nor mise is available"
fi

if command -v gpg >/dev/null 2>&1; then
  if gpg --list-secret-keys --keyid-format LONG 2>/dev/null | grep -q '^sec'; then
    ok "at least one GPG secret key is available locally"
  else
    fail "no local GPG secret key found; create or import one before signed releases"
  fi
else
  fail "gpg is not installed"
fi

credentials_via_env=false
if [[ -n "${MAVEN_CENTRAL_USERNAME:-}" && -n "${MAVEN_CENTRAL_PASSWORD:-}" ]]; then
  credentials_via_env=true
  ok "Central credentials detected via environment variables"
fi

credentials_via_settings=false
if xml_server_present "$SETTINGS_FILE"; then
  credentials_via_settings=true
  ok "Central credentials detected in ${SETTINGS_FILE}"
elif xml_server_present "${HOME}/.m2/settings.xml"; then
  credentials_via_settings=true
  ok "Central credentials detected in ${HOME}/.m2/settings.xml"
fi

if [[ "$credentials_via_env" == false && "$credentials_via_settings" == false ]]; then
  message="Central credentials not found via env or settings.xml"
  if [[ "$REQUIRE_CREDENTIALS" == true ]]; then
    fail "${message}"
  else
    warn "${message}"
  fi
fi

if [[ -n "${MAVEN_GPG_PASSPHRASE:-}" ]]; then
  ok "GPG passphrase detected via environment"
else
  warn "MAVEN_GPG_PASSPHRASE is not set in the environment"
fi

if command -v gh >/dev/null 2>&1; then
  if gh auth status >/dev/null 2>&1; then
    ok "gh CLI is authenticated"
  else
    warn "gh CLI is installed but not authenticated"
  fi
else
  warn "gh CLI is not installed; GitHub secret automation will be unavailable"
fi

echo
if (( failures > 0 )); then
  printf 'central-preflight: %d failure(s), %d warning(s)\n' "$failures" "$warnings" >&2
  exit 1
fi

printf 'central-preflight: ready with %d warning(s)\n' "$warnings"
