#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail

DOMAIN=""
VERIFICATION_KEY=""

usage() {
  cat <<'EOF'
Usage: check-namespace-dns.sh --domain DOMAIN --verification-key KEY

Checks whether the exact DNS domain queried by Sonatype contains the expected TXT
verification key.

Example:
  ./scripts/release/check-namespace-dns.sh \
    --domain cominotti.dev \
    --verification-key 12345678-abcd-1234-abcd-1234567890ab
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --domain)
      DOMAIN="$2"
      shift 2
      ;;
    --verification-key)
      VERIFICATION_KEY="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "central-dns-check: unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

: "${DOMAIN:?--domain is required}"
: "${VERIFICATION_KEY:?--verification-key is required}"

lookup_with_dig() {
  dig +short TXT "$DOMAIN" | sed 's/^"//; s/"$//; s/" "//g'
}

lookup_with_host() {
  host -t TXT "$DOMAIN" | sed -n 's/.* descriptive text "\(.*\)"/\1/p'
}

lookup_with_nslookup() {
  nslookup -type=TXT "$DOMAIN" 2>/dev/null | sed -n 's/.*text = "\(.*\)"/\1/p'
}

if command -v dig >/dev/null 2>&1; then
  records="$(lookup_with_dig)"
elif command -v host >/dev/null 2>&1; then
  records="$(lookup_with_host)"
elif command -v nslookup >/dev/null 2>&1; then
  records="$(lookup_with_nslookup)"
else
  echo "central-dns-check: need one of dig, host, or nslookup" >&2
  exit 2
fi

echo "Queried exact domain: ${DOMAIN}"

if [[ -z "$records" ]]; then
  echo "No TXT records were returned." >&2
  echo "Sonatype checks the exact domain only, so verify that the record was added to ${DOMAIN}." >&2
  exit 1
fi

echo "TXT records found:"
while IFS= read -r record; do
  printf '  - %s\n' "$record"
done <<< "$records"

if grep -Fxq "$VERIFICATION_KEY" <<< "$records"; then
  echo "central-dns-check: verification key is present"
  exit 0
fi

echo "central-dns-check: verification key not found" >&2
exit 1
