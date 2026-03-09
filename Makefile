# SPDX-License-Identifier: Apache-2.0

SHELL := /usr/bin/env bash
.SHELLFLAGS := -eu -o pipefail -c

.PHONY: help verify license-check license-fix sonar \
	central-preflight central-render-settings central-check-dns central-upload-gh-secrets central-dry-run

help:
	@echo "Targets:"
	@echo "  license-check  Maven validate guardrail for Apache-2.0 SPDX headers"
	@echo "  license-fix    Maven profile to auto-apply missing Apache-2.0 SPDX headers"
	@echo "  verify         Run Maven verify (includes license-check via validate)"
	@echo "  sonar          Run SonarCloud analysis via Maven (requires SONAR_TOKEN env var)"
	@echo "  central-preflight      Check local Maven Central prerequisites"
	@echo "  central-render-settings Render ~/.m2/settings-central.xml from env vars"
	@echo "  central-check-dns      Verify Sonatype TXT record presence (requires DOMAIN and VERIFICATION_KEY)"
	@echo "  central-upload-gh-secrets Upload release secrets with gh CLI"
	@echo "  central-dry-run        Run the release-only Maven profile without publishing"

license-check:
	mvn -B -ntp validate

license-fix:
	mvn -B -ntp -Plicense-fix validate

verify:
	mvn -B -ntp verify

sonar:
	: "$${SONAR_TOKEN:?SONAR_TOKEN must be exported in the environment}"
	mvn -B -ntp verify sonar:sonar -Dsonar.token="$${SONAR_TOKEN}"

central-preflight:
	./scripts/release/check-central-prereqs.sh

central-render-settings:
	./scripts/release/render-maven-settings.sh

central-check-dns:
	: "$${DOMAIN:?DOMAIN must be exported in the environment}"
	: "$${VERIFICATION_KEY:?VERIFICATION_KEY must be exported in the environment}"
	./scripts/release/check-namespace-dns.sh --domain "$${DOMAIN}" --verification-key "$${VERIFICATION_KEY}"

central-upload-gh-secrets:
	./scripts/release/upload-github-secrets.sh

central-dry-run:
	mvn -B -ntp -Pcentral-release -Dgpg.skip=true verify
