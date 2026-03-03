// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;

public final class TreeUtils {
  private TreeUtils() {}

  public static TreePosition getPosition(
      CompilationUnitTree compilationUnit, SourcePositions sourcePositions, Tree tree) {
    if (tree == null) {
      return new TreePosition(0, 0);
    }

    var startPosition = sourcePositions.getStartPosition(compilationUnit, tree);
    if (startPosition < 0) {
      return new TreePosition(0, 0);
    }

    var lineMap = compilationUnit.getLineMap();
    if (lineMap == null) {
      return new TreePosition(0, 0);
    }

    var line = lineMap.getLineNumber(startPosition);
    var column = lineMap.getColumnNumber(startPosition);
    return new TreePosition((int) line, (int) column);
  }
}
