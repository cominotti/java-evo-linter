# SPDX-License-Identifier: Apache-2.0

SHELL := /usr/bin/env bash
.SHELLFLAGS := -eu -o pipefail -c

.PHONY: help verify license-check license-fix sonar

help:
	@echo "Targets:"
	@echo "  license-check  Maven validate guardrail for Apache-2.0 SPDX headers"
	@echo "  license-fix    Maven profile to auto-apply missing Apache-2.0 SPDX headers"
	@echo "  verify         Run Maven verify (includes license-check via validate)"
	@echo "  sonar          Run SonarCloud analysis via Maven (requires SONAR_TOKEN env var)"

license-check:
	mvn -B -ntp validate

license-fix:
	mvn -B -ntp -Plicense-fix validate

verify:
	mvn -B -ntp verify

sonar:
	: "$${SONAR_TOKEN:?SONAR_TOKEN must be exported in the environment}"
	mvn -B -ntp verify sonar:sonar -Dsonar.token="$${SONAR_TOKEN}"
