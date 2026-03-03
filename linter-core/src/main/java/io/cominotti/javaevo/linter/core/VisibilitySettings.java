package io.cominotti.javaevo.linter.core;

public final class VisibilitySettings {
  public VisibilityPolicy fields = new VisibilityPolicy();
  public VisibilityPolicy methods = new VisibilityPolicy();

  public VisibilitySettings copy() {
    var copy = new VisibilitySettings();
    copy.fields = fields == null ? new VisibilityPolicy() : fields.copy();
    copy.methods = methods == null ? new VisibilityPolicy() : methods.copy();
    return copy;
  }
}
