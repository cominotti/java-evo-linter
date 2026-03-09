#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail

OUTPUT_PATH="${HOME}/.m2/settings-central.xml"
WRITE_STDOUT=false
FORCE=false

usage() {
  cat <<'EOF'
Usage: render-maven-settings.sh [options]

Renders a Maven settings file containing the Sonatype Central server credentials.

Required environment variables:
  MAVEN_CENTRAL_USERNAME
  MAVEN_CENTRAL_PASSWORD

Options:
  --output PATH   Output file path. Default: ~/.m2/settings-central.xml
  --stdout        Print the settings XML to stdout instead of writing a file
  --force         Overwrite an existing output file
  -h, --help      Show this help
EOF
}

xml_escape() {
  sed \
    -e 's/&/\&amp;/g' \
    -e 's/</\&lt;/g' \
    -e 's/>/\&gt;/g' \
    -e "s/'/\&apos;/g" \
    -e 's/"/\&quot;/g'
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --output)
      OUTPUT_PATH="$2"
      shift 2
      ;;
    --stdout)
      WRITE_STDOUT=true
      shift
      ;;
    --force)
      FORCE=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "render-settings: unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

: "${MAVEN_CENTRAL_USERNAME:?MAVEN_CENTRAL_USERNAME must be set}"
: "${MAVEN_CENTRAL_PASSWORD:?MAVEN_CENTRAL_PASSWORD must be set}"

escaped_username="$(printf '%s' "$MAVEN_CENTRAL_USERNAME" | xml_escape)"
escaped_password="$(printf '%s' "$MAVEN_CENTRAL_PASSWORD" | xml_escape)"

settings_xml="$(cat <<EOF
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>central</id>
      <username>${escaped_username}</username>
      <password>${escaped_password}</password>
    </server>
  </servers>
</settings>
EOF
)"

if [[ "$WRITE_STDOUT" == true ]]; then
  printf '%s\n' "$settings_xml"
  exit 0
fi

if [[ -f "$OUTPUT_PATH" && "$FORCE" == false ]]; then
  echo "render-settings: ${OUTPUT_PATH} already exists; rerun with --force to overwrite" >&2
  exit 1
fi

mkdir -p "$(dirname "$OUTPUT_PATH")"
printf '%s\n' "$settings_xml" > "$OUTPUT_PATH"
chmod 600 "$OUTPUT_PATH"
printf 'render-settings: wrote %s\n' "$OUTPUT_PATH"
