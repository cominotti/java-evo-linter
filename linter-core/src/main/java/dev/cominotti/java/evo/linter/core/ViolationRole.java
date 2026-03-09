// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

/** Semantic surface on which a finding was detected. */
public enum ViolationRole {
  FIELD_TYPE,
  METHOD_RETURN_TYPE,
  METHOD_PARAMETER_TYPE,
  RECORD_COMPONENT_TYPE
}
