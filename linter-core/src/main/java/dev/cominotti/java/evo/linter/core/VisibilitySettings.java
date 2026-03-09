// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

/** Visibility policies applied to fields and methods. */
public record VisibilitySettings(VisibilityPolicy fields, VisibilityPolicy methods) {
  public VisibilitySettings {
    fields = fields == null ? new VisibilityPolicy() : fields;
    methods = methods == null ? new VisibilityPolicy() : methods;
  }

  public VisibilitySettings() {
    this(new VisibilityPolicy(), new VisibilityPolicy());
  }

  public VisibilitySettings withFields(VisibilityPolicy value) {
    return new VisibilitySettings(value, methods);
  }

  public VisibilitySettings withMethods(VisibilityPolicy value) {
    return new VisibilitySettings(fields, value);
  }
}
