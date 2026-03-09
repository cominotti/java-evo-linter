#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail

REPO=""
GPG_KEY_FILE=""
GPG_KEY_ID=""

usage() {
  cat <<'EOF'
Usage: upload-github-secrets.sh [options]

Uploads the Maven Central release secrets to the current GitHub repository.

Required environment variables:
  MAVEN_CENTRAL_USERNAME
  MAVEN_CENTRAL_PASSWORD
  MAVEN_GPG_PASSPHRASE

Provide the private key in one of three ways:
  1. MAVEN_GPG_PRIVATE_KEY in the environment
  2. --gpg-key-file /path/to/private-key.asc
  3. --gpg-key-id KEYID to export it from the local GPG keyring

Options:
  --repo OWNER/REPO      Override the target repository
  --gpg-key-file PATH    Read the armored private key from a file
  --gpg-key-id KEYID     Export the armored private key from the local keyring
  -h, --help             Show this help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo)
      REPO="$2"
      shift 2
      ;;
    --gpg-key-file)
      GPG_KEY_FILE="$2"
      shift 2
      ;;
    --gpg-key-id)
      GPG_KEY_ID="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "github-secrets: unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

: "${MAVEN_CENTRAL_USERNAME:?MAVEN_CENTRAL_USERNAME must be set}"
: "${MAVEN_CENTRAL_PASSWORD:?MAVEN_CENTRAL_PASSWORD must be set}"
: "${MAVEN_GPG_PASSPHRASE:?MAVEN_GPG_PASSPHRASE must be set}"

if ! command -v gh >/dev/null 2>&1; then
  echo "github-secrets: gh CLI is required" >&2
  exit 2
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "github-secrets: gh CLI is not authenticated" >&2
  exit 1
fi

if [[ -z "$REPO" ]]; then
  REPO="$(gh repo view --json nameWithOwner --jq .nameWithOwner)"
fi

gpg_private_key="${MAVEN_GPG_PRIVATE_KEY:-}"
if [[ -z "$gpg_private_key" && -n "$GPG_KEY_FILE" ]]; then
  gpg_private_key="$(cat "$GPG_KEY_FILE")"
fi
if [[ -z "$gpg_private_key" && -n "$GPG_KEY_ID" ]]; then
  if ! command -v gpg >/dev/null 2>&1; then
    echo "github-secrets: gpg is required when using --gpg-key-id" >&2
    exit 2
  fi
  gpg_private_key="$(gpg --armor --export-secret-keys "$GPG_KEY_ID")"
fi
if [[ -z "$gpg_private_key" ]]; then
  echo "github-secrets: provide MAVEN_GPG_PRIVATE_KEY, --gpg-key-file, or --gpg-key-id" >&2
  exit 1
fi

set_secret() {
  local name="$1"
  local value="$2"
  printf '%s' "$value" | gh secret set "$name" --repo "$REPO"
  printf 'github-secrets: set %s on %s\n' "$name" "$REPO"
}

set_secret MAVEN_CENTRAL_USERNAME "$MAVEN_CENTRAL_USERNAME"
set_secret MAVEN_CENTRAL_PASSWORD "$MAVEN_CENTRAL_PASSWORD"
set_secret MAVEN_GPG_PRIVATE_KEY "$gpg_private_key"
set_secret MAVEN_GPG_PASSPHRASE "$MAVEN_GPG_PASSPHRASE"
