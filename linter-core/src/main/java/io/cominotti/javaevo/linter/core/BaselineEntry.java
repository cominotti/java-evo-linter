// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

public record BaselineEntry(
    @JsonProperty("finding_id") String findingId,
    @JsonProperty("rule_id") String ruleId,
    @JsonProperty("justification") @Nullable String justification,
    @JsonProperty("owner") @Nullable String owner,
    @JsonProperty("expires_on") @Nullable String expiresOn) {}
