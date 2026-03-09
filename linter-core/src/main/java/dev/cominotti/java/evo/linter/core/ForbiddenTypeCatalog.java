// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import org.jspecify.annotations.Nullable;

final class ForbiddenTypeCatalog {
  private static final Set<String> DEFAULT_PRIMITIVES =
      Set.of("boolean", "byte", "short", "int", "long", "float", "double", "char");

  private static final Map<String, String> SIMPLE_TO_FQCN =
      Map.ofEntries(
          Map.entry("Boolean", "java.lang.Boolean"),
          Map.entry("Byte", "java.lang.Byte"),
          Map.entry("Short", "java.lang.Short"),
          Map.entry("Integer", "java.lang.Integer"),
          Map.entry("Long", "java.lang.Long"),
          Map.entry("Float", "java.lang.Float"),
          Map.entry("Double", "java.lang.Double"),
          Map.entry("Character", "java.lang.Character"),
          Map.entry("String", "java.lang.String"));

  private final Set<String> forbiddenPrimitives;
  private final Set<String> forbiddenDeclaredTypes;

  public ForbiddenTypeCatalog(List<String> configuredForbiddenTypes) {
    var primitiveAccumulator = new LinkedHashSet<String>();
    var declaredAccumulator = new LinkedHashSet<String>();

    for (String configuredForbiddenType : configuredForbiddenTypes) {
      String normalized = normalizeConfiguredType(configuredForbiddenType);
      if (DEFAULT_PRIMITIVES.contains(normalized)) {
        primitiveAccumulator.add(normalized);
      } else {
        declaredAccumulator.add(normalized);
      }
    }

    forbiddenPrimitives = Set.copyOf(primitiveAccumulator);
    forbiddenDeclaredTypes = Set.copyOf(declaredAccumulator);
  }

  public static List<String> defaultForbiddenTypes() {
    var defaults = new ArrayList<String>(DEFAULT_PRIMITIVES);
    defaults.addAll(SIMPLE_TO_FQCN.values());
    Collections.sort(defaults);
    return defaults;
  }

  public static String normalizeConfiguredType(String raw) {
    if (raw == null) {
      throw new IllegalArgumentException("Forbidden type cannot be null");
    }
    var trimmed = raw.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("Forbidden type cannot be blank");
    }

    var lower = trimmed.toLowerCase(Locale.ROOT);
    if (DEFAULT_PRIMITIVES.contains(lower)) {
      return lower;
    }

    var fqcn = SIMPLE_TO_FQCN.get(trimmed);
    if (fqcn != null) {
      return fqcn;
    }

    if (SIMPLE_TO_FQCN.containsValue(trimmed)) {
      return trimmed;
    }

    return trimmed;
  }

  public Set<String> collectForbiddenMatches(TypeMirror typeMirror) {
    var matches = new LinkedHashSet<String>();
    if (typeMirror == null) {
      return matches;
    }
    collectForbiddenMatchesRecursive(
        typeMirror, matches, Collections.newSetFromMap(new IdentityHashMap<>()));
    return matches;
  }

  private void collectForbiddenMatchesRecursive(
      @Nullable TypeMirror typeMirror, Set<String> matches, Set<TypeMirror> visited) {
    if (typeMirror == null || !visited.add(typeMirror)) {
      return;
    }

    var kind = typeMirror.getKind();
    if (kind.isPrimitive()) {
      var primitiveName = kind.name().toLowerCase(Locale.ROOT);
      if (forbiddenPrimitives.contains(primitiveName)) {
        matches.add(primitiveName);
      }
      return;
    }

    switch (kind) {
      case ARRAY ->
          collectForbiddenMatchesRecursive(
              ((ArrayType) typeMirror).getComponentType(), matches, visited);
      case DECLARED -> collectFromDeclaredType((DeclaredType) typeMirror, matches, visited);
      case ERROR -> collectFromErrorType((ErrorType) typeMirror, matches);
      case TYPEVAR -> collectFromTypeVariable((TypeVariable) typeMirror, matches, visited);
      case WILDCARD -> collectFromWildcardType((WildcardType) typeMirror, matches, visited);
      case UNION -> collectFromMany(((UnionType) typeMirror).getAlternatives(), matches, visited);
      case INTERSECTION ->
          collectFromMany(((IntersectionType) typeMirror).getBounds(), matches, visited);
      default -> {
        // No-op for NONE, VOID, EXECUTABLE, PACKAGE, MODULE, etc.
      }
    }
  }

  private void collectFromDeclaredType(
      DeclaredType declaredType, Set<String> matches, Set<TypeMirror> visited) {
    collectTypeIfForbidden(declaredType.asElement(), matches);
    collectFromMany(declaredType.getTypeArguments(), matches, visited);
  }

  private void collectFromErrorType(ErrorType errorType, Set<String> matches) {
    collectTypeIfForbidden(errorType.asElement(), matches);
  }

  private void collectFromTypeVariable(
      TypeVariable typeVariable, Set<String> matches, Set<TypeMirror> visited) {
    collectForbiddenMatchesRecursive(typeVariable.getUpperBound(), matches, visited);
    collectForbiddenMatchesRecursive(typeVariable.getLowerBound(), matches, visited);
  }

  private void collectFromWildcardType(
      WildcardType wildcardType, Set<String> matches, Set<TypeMirror> visited) {
    collectForbiddenMatchesRecursive(wildcardType.getExtendsBound(), matches, visited);
    collectForbiddenMatchesRecursive(wildcardType.getSuperBound(), matches, visited);
  }

  private void collectFromMany(
      List<? extends TypeMirror> mirrors, Set<String> matches, Set<TypeMirror> visited) {
    for (TypeMirror mirror : mirrors) {
      collectForbiddenMatchesRecursive(mirror, matches, visited);
    }
  }

  private void collectTypeIfForbidden(Object element, Set<String> matches) {
    if (element instanceof TypeElement typeElement) {
      var qualifiedName = typeElement.getQualifiedName().toString();
      if (forbiddenDeclaredTypes.contains(qualifiedName)) {
        matches.add(qualifiedName);
      }
    }
  }
}
