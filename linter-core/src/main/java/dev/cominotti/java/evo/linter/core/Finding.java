// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Comparator;

/** A single linter finding serialized in reports and JSONL output. */
public record Finding(
    @JsonProperty("schema_version") int schemaVersion,
    @JsonProperty("rule_id") String ruleId,
    @JsonProperty("finding_id") String findingId,
    @JsonProperty("severity") String severity,
    @JsonProperty("file") String file,
    @JsonProperty("line") int line,
    @JsonProperty("column") int column,
    @JsonProperty("package") String packageName,
    @JsonProperty("owner_type") String ownerType,
    @JsonProperty("member_kind") String memberKind,
    @JsonProperty("member_signature") String memberSignature,
    @JsonProperty("visibility") String visibility,
    @JsonProperty("violation_role") String violationRole,
    @JsonProperty("forbidden_type") String forbiddenType,
    @JsonProperty("declared_type") String declaredType,
    @JsonProperty("message") String message,
    @JsonProperty("suggestion") String suggestion) {
  public static final int SCHEMA_VERSION = 1;

  public static final Comparator<Finding> ORDERING =
      Comparator.comparing(Finding::file)
          .thenComparingInt(Finding::line)
          .thenComparingInt(Finding::column)
          .thenComparing(Finding::findingId);
}
