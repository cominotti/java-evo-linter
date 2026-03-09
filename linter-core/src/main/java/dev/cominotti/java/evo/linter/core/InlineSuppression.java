// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.NewArrayTree;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class InlineSuppression {
  private static final Set<String> GENERIC_KEYS = Set.of("all", "java-evo-linter:all");

  private InlineSuppression() {}

  public static boolean isSuppressed(
      List<? extends AnnotationTree> annotations, Collection<String> configuredKeys) {
    if (annotations == null || annotations.isEmpty()) {
      return false;
    }

    var normalizedConfigured = new HashSet<String>();
    for (String configuredKey : configuredKeys) {
      if (configuredKey != null && !configuredKey.isBlank()) {
        normalizedConfigured.add(configuredKey.trim().toLowerCase(Locale.ROOT));
      }
    }

    for (AnnotationTree annotation : annotations) {
      if (!isSuppressWarningsAnnotation(annotation)) {
        continue;
      }
      for (String key : extractSuppressionKeys(annotation)) {
        var normalized = key.toLowerCase(Locale.ROOT);
        if (GENERIC_KEYS.contains(normalized) || normalizedConfigured.contains(normalized)) {
          return true;
        }
      }
    }

    return false;
  }

  private static boolean isSuppressWarningsAnnotation(AnnotationTree annotation) {
    var typeName = annotation.getAnnotationType().toString();
    return "SuppressWarnings".equals(typeName) || "java.lang.SuppressWarnings".equals(typeName);
  }

  private static Set<String> extractSuppressionKeys(AnnotationTree annotation) {
    var keys = new HashSet<String>();
    for (ExpressionTree argument : annotation.getArguments()) {
      if (argument instanceof AssignmentTree assignmentTree) {
        collectKeys(assignmentTree.getExpression(), keys);
      } else {
        collectKeys(argument, keys);
      }
    }
    return keys;
  }

  private static void collectKeys(ExpressionTree expressionTree, Set<String> keys) {
    if (expressionTree instanceof LiteralTree literalTree) {
      var value = literalTree.getValue();
      if (value instanceof String stringValue) {
        keys.add(stringValue);
      }
      return;
    }

    if (expressionTree instanceof NewArrayTree arrayTree) {
      if (arrayTree.getInitializers() == null) {
        return;
      }
      for (ExpressionTree initializer : arrayTree.getInitializers()) {
        collectKeys(initializer, keys);
      }
    }
  }
}
