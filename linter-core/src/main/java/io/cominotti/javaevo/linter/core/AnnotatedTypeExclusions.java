// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class AnnotatedTypeExclusions {
  private static final List<String> DEFAULT_OWNER_ANNOTATIONS = List.of("EnterpriseValueObject");

  public List<String> fieldLikeOwnerAnnotations = new ArrayList<>(DEFAULT_OWNER_ANNOTATIONS);
  public List<String> parameterOwnerAnnotations = new ArrayList<>(DEFAULT_OWNER_ANNOTATIONS);

  public void normalize() {
    fieldLikeOwnerAnnotations = normalizeList(fieldLikeOwnerAnnotations, DEFAULT_OWNER_ANNOTATIONS);
    parameterOwnerAnnotations = normalizeList(parameterOwnerAnnotations, DEFAULT_OWNER_ANNOTATIONS);
  }

  public AnnotatedTypeExclusions copy() {
    var copy = new AnnotatedTypeExclusions();
    copy.fieldLikeOwnerAnnotations =
        fieldLikeOwnerAnnotations == null
            ? new ArrayList<>(DEFAULT_OWNER_ANNOTATIONS)
            : new ArrayList<>(fieldLikeOwnerAnnotations);
    copy.parameterOwnerAnnotations =
        parameterOwnerAnnotations == null
            ? new ArrayList<>(DEFAULT_OWNER_ANNOTATIONS)
            : new ArrayList<>(parameterOwnerAnnotations);
    return copy;
  }

  private static List<String> normalizeList(
      @Nullable List<String> configuredValues, List<String> defaults) {
    if (configuredValues == null) {
      return new ArrayList<>(defaults);
    }

    var normalized = new LinkedHashSet<String>();
    for (String configuredValue : configuredValues) {
      normalizeAnnotationName(configuredValue)
          .filter(value -> !value.isBlank())
          .ifPresent(normalized::add);
    }

    return new ArrayList<>(normalized);
  }

  static Optional<String> normalizeAnnotationName(@Nullable String rawName) {
    if (rawName == null) {
      return Optional.empty();
    }

    var normalized = rawName.trim();
    while (normalized.startsWith("@")) {
      normalized = normalized.substring(1).trim();
    }

    return Optional.of(normalized);
  }
}
