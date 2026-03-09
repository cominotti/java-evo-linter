// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Owner-annotation names that suppress field-like and parameter findings. */
public record AnnotatedTypeExclusions(
    List<String> fieldLikeOwnerAnnotations, List<String> parameterOwnerAnnotations) {
  private static final List<String> DEFAULT_OWNER_ANNOTATIONS = List.of("EvoType");

  public AnnotatedTypeExclusions {
    fieldLikeOwnerAnnotations = normalizeList(fieldLikeOwnerAnnotations, DEFAULT_OWNER_ANNOTATIONS);
    parameterOwnerAnnotations = normalizeList(parameterOwnerAnnotations, DEFAULT_OWNER_ANNOTATIONS);
  }

  public AnnotatedTypeExclusions() {
    this(DEFAULT_OWNER_ANNOTATIONS, DEFAULT_OWNER_ANNOTATIONS);
  }

  public AnnotatedTypeExclusions withFieldLikeOwnerAnnotations(List<String> value) {
    return new AnnotatedTypeExclusions(value, parameterOwnerAnnotations);
  }

  public AnnotatedTypeExclusions withParameterOwnerAnnotations(List<String> value) {
    return new AnnotatedTypeExclusions(fieldLikeOwnerAnnotations, value);
  }

  private static List<String> normalizeList(
      @Nullable List<String> configuredValues, List<String> defaults) {
    if (configuredValues == null) {
      return List.copyOf(defaults);
    }

    var normalized = new LinkedHashSet<String>();
    for (String configuredValue : configuredValues) {
      normalizeAnnotationName(configuredValue)
          .filter(value -> !value.isBlank())
          .ifPresent(normalized::add);
    }

    return List.copyOf(new ArrayList<>(normalized));
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
