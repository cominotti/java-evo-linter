# SPDX-License-Identifier: Apache-2.0

SHELL := /usr/bin/env bash
.SHELLFLAGS := -eu -o pipefail -c

.PHONY: help verify license-check license-fix

help:
	@echo "Targets:"
	@echo "  license-check  Maven validate guardrail for Apache-2.0 SPDX headers"
	@echo "  license-fix    Maven profile to auto-apply missing Apache-2.0 SPDX headers"
	@echo "  verify         Run Maven verify (includes license-check via validate)"

license-check:
	mvn -B -ntp validate

license-fix:
	mvn -B -ntp -Plicense-fix validate

verify:
	mvn -B -ntp verify
