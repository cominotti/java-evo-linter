// SPDX-License-Identifier: Apache-2.0

package dev.cominotti.java.evo.linter.core;

/** Checked exception raised when a linter run cannot complete successfully. */
public final class LinterException extends Exception {
  public LinterException(String message) {
    super(message);
  }

  public LinterException(String message, Throwable cause) {
    super(message, cause);
  }
}
