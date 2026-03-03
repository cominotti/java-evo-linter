package io.cominotti.javaevo.linter.core;

public final class LinterException extends Exception {
  public LinterException(String message) {
    super(message);
  }

  public LinterException(String message, Throwable cause) {
    super(message, cause);
  }
}
