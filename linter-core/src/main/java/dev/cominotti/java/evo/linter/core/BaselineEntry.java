// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/** Persisted baseline entry used to suppress pre-existing findings. */
public record BaselineEntry(
    @JsonProperty("finding_id") String findingId,
    @JsonProperty("rule_id") String ruleId,
    @JsonProperty("justification") @Nullable String justification,
    @JsonProperty("owner") @Nullable String owner,
    @JsonProperty("expires_on") @Nullable String expiresOn) {}
