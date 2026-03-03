// SPDX-License-Identifier: Apache-2.0

package io.cominotti.javaevo.linter.core;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

public final class PrimitiveBoxedSignatureScanner {
  private final Path projectRoot;
  private final LinterConfig config;
  private final ForbiddenTypeCatalog forbiddenTypeCatalog;
  private final VisibilityResolver visibilityResolver;
  private final AnnotationNameMatcher fieldLikeOwnerAnnotationMatcher;
  private final AnnotationNameMatcher parameterOwnerAnnotationMatcher;

  public PrimitiveBoxedSignatureScanner(Path projectRoot, LinterConfig config) {
    this.projectRoot = projectRoot;
    this.config = config;
    this.forbiddenTypeCatalog = new ForbiddenTypeCatalog(config.forbiddenTypes());
    this.visibilityResolver =
        new VisibilityResolver(config.visibility(), config.packageOverrides());
    AnnotatedTypeExclusions exclusions =
        config.annotatedTypeExclusions() == null
            ? new AnnotatedTypeExclusions()
            : config.annotatedTypeExclusions();
    this.fieldLikeOwnerAnnotationMatcher =
        AnnotationNameMatcher.fromConfigured(exclusions.fieldLikeOwnerAnnotations());
    this.parameterOwnerAnnotationMatcher =
        AnnotationNameMatcher.fromConfigured(exclusions.parameterOwnerAnnotations());
  }

  public ScanReport scan() throws LinterException {
    List<Path> sourceFiles = discoverSourceFiles();
    if (sourceFiles.isEmpty()) {
      return new ScanReport(List.of(), 0, 0);
    }

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new LinterException("JDK compiler not available. Run with a full JDK (not JRE)");
    }

    var diagnostics = new DiagnosticCollector<JavaFileObject>();
    try (var fileManager =
        compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
      configureLocations(fileManager);

      var javaFiles = fileManager.getJavaFileObjectsFromPaths(sourceFiles);
      var options = List.of("-proc:none", "-Xlint:none");

      var task =
          (JavacTask) compiler.getTask(null, fileManager, diagnostics, options, null, javaFiles);

      var compilationUnits = parseAndAnalyze(task);

      failOnCompileErrorsIfConfigured(diagnostics);

      Trees trees = Trees.instance(task);
      var counts = new MutableCounts();
      var findings = new ArrayList<Finding>();

      for (CompilationUnitTree compilationUnit : compilationUnits) {
        var scanner = new CompilationScanner(compilationUnit, trees, findings, counts);
        scanner.scan(compilationUnit, null);
      }

      findings.sort(Finding.ORDERING);
      return new ScanReport(
          List.copyOf(findings), counts.rawFindings, counts.inlineSuppressedFindings);
    } catch (IOException exception) {
      throw new LinterException("Failed to scan Java sources", exception);
    }
  }

  private void configureLocations(StandardJavaFileManager fileManager) throws IOException {
    var sourceRoots = LinterConfigLoader.normalizePaths(projectRoot, config.sourceRoots());
    var existingSourceRoots = new ArrayList<Path>();
    for (Path sourceRoot : sourceRoots) {
      if (Files.isDirectory(sourceRoot)) {
        existingSourceRoots.add(sourceRoot);
      }
    }
    if (!existingSourceRoots.isEmpty()) {
      fileManager.setLocationFromPaths(StandardLocation.SOURCE_PATH, existingSourceRoots);
    }

    if (config.classpath() != null && !config.classpath().isEmpty()) {
      var classpathEntries = LinterConfigLoader.normalizePaths(projectRoot, config.classpath());
      fileManager.setLocationFromPaths(StandardLocation.CLASS_PATH, classpathEntries);
    }
  }

  private void failOnCompileErrorsIfConfigured(DiagnosticCollector<JavaFileObject> diagnostics)
      throws LinterException {
    if (!Boolean.TRUE.equals(config.failOnCompileErrors())) {
      return;
    }

    List<Diagnostic<? extends JavaFileObject>> errors =
        diagnostics.getDiagnostics().stream()
            .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
            .toList();
    if (errors.isEmpty()) {
      return;
    }

    var message = new StringBuilder("Compilation errors detected during analysis:\n");
    var max = Math.min(errors.size(), 20);
    for (var index = 0; index < max; index++) {
      var error = errors.get(index);
      String source = error.getSource() == null ? "<unknown>" : error.getSource().getName();
      message
          .append(" - ")
          .append(source)
          .append(":")
          .append(error.getLineNumber())
          .append(":")
          .append(error.getColumnNumber())
          .append(" ")
          .append(error.getMessage(Locale.ROOT))
          .append('\n');
    }
    if (errors.size() > max) {
      message.append(" - ... and ").append(errors.size() - max).append(" more\n");
    }

    throw new LinterException(message.toString().trim());
  }

  private List<Path> discoverSourceFiles() throws LinterException {
    var includeMatchers = toPathMatchers(config.includeGlobs());
    var excludeMatchers = toPathMatchers(config.excludeGlobs());

    var sourceFiles = new LinkedHashSet<Path>();
    for (String rootEntry : config.sourceRoots()) {
      Path sourceRoot = LinterConfigLoader.normalizePath(projectRoot, Path.of(rootEntry));
      if (Files.isDirectory(sourceRoot)) {
        collectSourceFilesFromRoot(sourceRoot, includeMatchers, excludeMatchers, sourceFiles);
      }
    }

    var sorted = new ArrayList<Path>(sourceFiles);
    Collections.sort(sorted);
    return sorted;
  }

  private Iterable<? extends CompilationUnitTree> parseAndAnalyze(JavacTask task)
      throws LinterException {
    try {
      var compilationUnits = task.parse();
      task.analyze();
      return compilationUnits;
    } catch (IOException exception) {
      throw new LinterException("Failed while parsing Java sources", exception);
    }
  }

  private void collectSourceFilesFromRoot(
      Path sourceRoot,
      List<PathMatcher> includeMatchers,
      List<PathMatcher> excludeMatchers,
      Set<Path> sourceFiles)
      throws LinterException {
    try (var sources = Files.walk(sourceRoot)) {
      sources
          .filter(
              path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java"))
          .forEach(
              path -> {
                relativizePath(path)
                    .filter(
                        relativePath ->
                            shouldInclude(relativePath, includeMatchers, excludeMatchers))
                    .ifPresent(_ -> sourceFiles.add(path.normalize()));
              });
    } catch (IOException exception) {
      throw new LinterException("Failed walking source root: " + sourceRoot, exception);
    }
  }

  private Optional<Path> relativizePath(Path path) {
    try {
      return Optional.of(projectRoot.relativize(path));
    } catch (IllegalArgumentException _) {
      return Optional.empty();
    }
  }

  private boolean shouldInclude(
      Path relativePath, List<PathMatcher> includeMatchers, List<PathMatcher> excludeMatchers) {
    var includeMatches = includeMatchers.isEmpty() || matchesAny(includeMatchers, relativePath);
    return includeMatches && !matchesAny(excludeMatchers, relativePath);
  }

  private List<PathMatcher> toPathMatchers(List<String> globs) throws LinterException {
    var matchers = new ArrayList<PathMatcher>();
    if (globs == null) {
      return matchers;
    }

    for (String glob : globs) {
      if (glob == null || glob.isBlank()) {
        continue;
      }
      try {
        matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob));
      } catch (IllegalArgumentException exception) {
        throw new LinterException("Invalid glob pattern: " + glob, exception);
      }
    }

    return matchers;
  }

  private boolean matchesAny(List<PathMatcher> matchers, Path relativePath) {
    if (matchers.isEmpty()) {
      return false;
    }
    for (PathMatcher matcher : matchers) {
      if (matcher.matches(relativePath)) {
        return true;
      }
    }
    return false;
  }

  private static final class MutableCounts {
    int rawFindings;
    int inlineSuppressedFindings;
  }

  private static final class AnnotationNameMatcher {
    private final Set<String> qualifiedNames;
    private final Set<String> simpleNames;

    private AnnotationNameMatcher(Set<String> qualifiedNames, Set<String> simpleNames) {
      this.qualifiedNames = qualifiedNames;
      this.simpleNames = simpleNames;
    }

    static AnnotationNameMatcher fromConfigured(List<String> configuredAnnotations) {
      var qualifiedNames = new LinkedHashSet<String>();
      var simpleNames = new LinkedHashSet<String>();

      if (configuredAnnotations != null) {
        for (String configuredAnnotation : configuredAnnotations) {
          AnnotatedTypeExclusions.normalizeAnnotationName(configuredAnnotation)
              .filter(value -> !value.isBlank())
              .ifPresent(
                  value -> {
                    if (value.contains(".")) {
                      qualifiedNames.add(value);
                    }
                    simpleNames.add(extractSimpleName(value));
                  });
        }
      }

      return new AnnotationNameMatcher(Set.copyOf(qualifiedNames), Set.copyOf(simpleNames));
    }

    boolean isEmpty() {
      return qualifiedNames.isEmpty() && simpleNames.isEmpty();
    }

    boolean matches(String annotationName) {
      return AnnotatedTypeExclusions.normalizeAnnotationName(annotationName)
          .filter(value -> !value.isBlank())
          .map(
              value ->
                  qualifiedNames.contains(value) || simpleNames.contains(extractSimpleName(value)))
          .orElse(false);
    }

    private static String extractSimpleName(String value) {
      var lastSeparator = Math.max(value.lastIndexOf('.'), value.lastIndexOf('$'));
      if (lastSeparator < 0) {
        return value;
      }
      return value.substring(lastSeparator + 1);
    }
  }

  private final class CompilationScanner extends TreePathScanner<Void, Void> {
    private final CompilationUnitTree compilationUnit;
    private final Trees trees;
    private final String packageName;
    private final String displayFile;
    private final List<Finding> findings;
    private final MutableCounts counts;

    private final Deque<String> ownerStack = new ArrayDeque<>();
    private final Deque<Boolean> classSuppressionStack = new ArrayDeque<>();
    private final Deque<Boolean> fieldLikeOwnerExclusionStack = new ArrayDeque<>();
    private final Deque<Boolean> parameterOwnerExclusionStack = new ArrayDeque<>();
    private final Deque<Set<String>> recordComponentNamesStack = new ArrayDeque<>();

    private CompilationScanner(
        CompilationUnitTree compilationUnit,
        Trees trees,
        List<Finding> findings,
        MutableCounts counts) {
      this.compilationUnit = compilationUnit;
      this.trees = trees;
      this.findings = findings;
      this.counts = counts;
      this.packageName =
          compilationUnit.getPackageName() == null
              ? ""
              : compilationUnit.getPackageName().toString();
      this.displayFile = relativizeSourcePath(compilationUnit);
    }

    @Override
    public Void visitClass(ClassTree node, Void unused) {
      var simpleName = node.getSimpleName().toString();
      if (simpleName.isBlank()) {
        TreePosition position =
            TreeUtils.getPosition(compilationUnit, trees.getSourcePositions(), node);
        simpleName = "<anonymous@" + position.line() + ">";
      }

      ownerStack.push(simpleName);
      var inheritedSuppression =
          classSuppressionStack.peek() != null && classSuppressionStack.peek();
      var classSuppressed =
          inheritedSuppression
              || (config.suppression().inlineEnabled()
                  && InlineSuppression.isSuppressed(
                      node.getModifiers().getAnnotations(), config.suppression().keys()));
      classSuppressionStack.push(classSuppressed);
      fieldLikeOwnerExclusionStack.push(
          ownerHasExcludedAnnotation(
              node.getModifiers().getAnnotations(), fieldLikeOwnerAnnotationMatcher));
      parameterOwnerExclusionStack.push(
          ownerHasExcludedAnnotation(
              node.getModifiers().getAnnotations(), parameterOwnerAnnotationMatcher));
      recordComponentNamesStack.push(resolveRecordComponentNames());

      try {
        return super.visitClass(node, null);
      } finally {
        recordComponentNamesStack.pop();
        parameterOwnerExclusionStack.pop();
        fieldLikeOwnerExclusionStack.pop();
        classSuppressionStack.pop();
        ownerStack.pop();
      }
    }

    @Override
    public Void visitVariable(VariableTree node, Void unused) {
      var path = getCurrentPath();
      Tree parent = path.getParentPath() == null ? null : path.getParentPath().getLeaf();
      if (parent instanceof ClassTree && node.getType() != null) {
        if (isCurrentOwnerRecordComponent(node)) {
          inspectRecordComponent(node);
        } else {
          inspectField(node);
        }
      }
      return super.visitVariable(node, null);
    }

    @Override
    public Void visitMethod(MethodTree node, Void unused) {
      inspectMethod(node);
      return super.visitMethod(node, null);
    }

    private void inspectField(VariableTree node) {
      if (isCurrentOwnerFieldLikeExcluded()) {
        return;
      }

      var visibility = resolveVisibility(node.getModifiers());
      if (!visibilityResolver.shouldCheckField(packageName, visibility)) {
        return;
      }

      var inlineSuppressed = isInlineSuppressed(node.getModifiers());
      var ownerType = buildOwnerType();
      var memberSignature = ownerType + "#" + node.getName();

      emitFindings(
          node.getType(),
          new TreePath(getCurrentPath(), node.getType()),
          new EmissionContext(
              ViolationRole.FIELD_TYPE,
              MemberKind.FIELD,
              ownerType,
              memberSignature,
              visibility,
              node.getName().toString(),
              inlineSuppressed));
    }

    private void inspectMethod(MethodTree node) {
      var visibility = resolveVisibility(node.getModifiers());
      if (!visibilityResolver.shouldCheckMethod(packageName, visibility)) {
        return;
      }

      var constructor = "<init>".contentEquals(node.getName());
      var inlineSuppressed = isInlineSuppressed(node.getModifiers());
      var ownerType = buildOwnerType();
      var methodSignature = buildMethodSignature(ownerType, node, constructor);

      if (!constructor && node.getReturnType() != null) {
        emitFindings(
            node.getReturnType(),
            new TreePath(getCurrentPath(), node.getReturnType()),
            new EmissionContext(
                ViolationRole.METHOD_RETURN_TYPE,
                MemberKind.METHOD,
                ownerType,
                methodSignature,
                visibility,
                "return",
                inlineSuppressed));
      }

      List<? extends VariableTree> parameters = node.getParameters();
      for (var index = 0; index < parameters.size(); index++) {
        if (!isCurrentOwnerParameterExcluded()) {
          var parameter = parameters.get(index);
          if (parameter.getType() != null) {
            var parameterPath = new TreePath(getCurrentPath(), parameter);
            emitFindings(
                parameter.getType(),
                new TreePath(parameterPath, parameter.getType()),
                new EmissionContext(
                    ViolationRole.METHOD_PARAMETER_TYPE,
                    constructor ? MemberKind.CONSTRUCTOR : MemberKind.METHOD,
                    ownerType,
                    methodSignature,
                    visibility,
                    "param:" + index,
                    inlineSuppressed));
          }
        }
      }
    }

    private void inspectRecordComponent(VariableTree recordComponent) {
      if (isCurrentOwnerFieldLikeExcluded()) {
        return;
      }

      if (recordComponent.getType() == null) {
        return;
      }

      var inlineSuppressed = isInlineSuppressed(recordComponent.getModifiers());
      var ownerType = buildOwnerType();
      var memberSignature = ownerType + "#<record>(" + recordComponent.getName() + ")";
      var componentPath = new TreePath(getCurrentPath(), recordComponent);
      emitFindings(
          recordComponent.getType(),
          new TreePath(componentPath, recordComponent.getType()),
          new EmissionContext(
              ViolationRole.RECORD_COMPONENT_TYPE,
              MemberKind.RECORD_COMPONENT,
              ownerType,
              memberSignature,
              Visibility.PUBLIC,
              recordComponent.getName().toString(),
              inlineSuppressed));
    }

    private void emitFindings(Tree typeTree, TreePath typePath, EmissionContext context) {
      var typeMirror = trees.getTypeMirror(typePath);
      var matches = forbiddenTypeCatalog.collectForbiddenMatches(typeMirror);
      if (matches.isEmpty()) {
        return;
      }

      for (String forbiddenType : matches) {
        counts.rawFindings++;
        if (context.inlineSuppressed()) {
          counts.inlineSuppressedFindings++;
          continue;
        }

        TreePosition position =
            TreeUtils.getPosition(compilationUnit, trees.getSourcePositions(), typeTree);
        String declaredType = typeMirror == null ? typeTree.toString() : typeMirror.toString();
        String fingerprintSource =
            String.join(
                "\u001f",
                RuleIds.PRIMITIVE_BOXED_SIGNATURE,
                packageName,
                context.ownerType(),
                context.memberKind().name(),
                context.memberSignature(),
                context.role().name(),
                context.roleQualifier(),
                forbiddenType);

        findings.add(
            new Finding(
                Finding.SCHEMA_VERSION,
                RuleIds.PRIMITIVE_BOXED_SIGNATURE,
                Hashing.sha256Hex(fingerprintSource),
                "error",
                displayFile,
                position.line(),
                position.column(),
                packageName,
                context.ownerType(),
                context.memberKind().name().toLowerCase(Locale.ROOT),
                context.memberSignature(),
                context.visibility().name().toLowerCase(Locale.ROOT),
                context.role().name().toLowerCase(Locale.ROOT),
                forbiddenType,
                declaredType,
                "Forbidden primitive/boxed type in production signature",
                "Replace with a domain value object or richer abstraction"));
      }
    }

    private String buildMethodSignature(String ownerType, MethodTree method, boolean constructor) {
      var signature =
          new StringBuilder(ownerType)
              .append('#')
              .append(constructor ? "<init>" : method.getName())
              .append('(');

      List<? extends VariableTree> parameters = method.getParameters();
      for (var index = 0; index < parameters.size(); index++) {
        if (index > 0) {
          signature.append(',');
        }
        var parameter = parameters.get(index);
        signature.append(parameter.getType() == null ? "<unknown>" : parameter.getType());
      }
      signature.append(')');

      if (!constructor && method.getReturnType() != null) {
        signature.append(':').append(method.getReturnType());
      }

      return signature.toString();
    }

    private record EmissionContext(
        ViolationRole role,
        MemberKind memberKind,
        String ownerType,
        String memberSignature,
        Visibility visibility,
        String roleQualifier,
        boolean inlineSuppressed) {}

    private String buildOwnerType() {
      var names = new ArrayList<String>(ownerStack);
      Collections.reverse(names);
      return String.join("$", names);
    }

    private boolean ownerHasExcludedAnnotation(
        List<? extends AnnotationTree> annotations, AnnotationNameMatcher matcher) {
      if (matcher.isEmpty() || annotations == null || annotations.isEmpty()) {
        return false;
      }

      for (AnnotationTree annotation : annotations) {
        if (matcher.matches(annotation.getAnnotationType().toString())) {
          return true;
        }

        var annotationTypePath = new TreePath(getCurrentPath(), annotation.getAnnotationType());
        var annotationMirror = trees.getTypeMirror(annotationTypePath);
        if (annotationMirror == null
            || annotationMirror.getKind() == TypeKind.ERROR
            || annotationMirror.getKind() == TypeKind.NONE) {
          continue;
        }

        if (matcher.matches(annotationMirror.toString())) {
          return true;
        }
      }

      return false;
    }

    private boolean isCurrentOwnerFieldLikeExcluded() {
      return fieldLikeOwnerExclusionStack.peek() != null && fieldLikeOwnerExclusionStack.peek();
    }

    private boolean isCurrentOwnerParameterExcluded() {
      return parameterOwnerExclusionStack.peek() != null && parameterOwnerExclusionStack.peek();
    }

    private boolean isCurrentOwnerRecordComponent(VariableTree node) {
      var recordComponentNames = recordComponentNamesStack.peek();
      return recordComponentNames != null
          && !recordComponentNames.isEmpty()
          && recordComponentNames.contains(node.getName().toString());
    }

    private Set<String> resolveRecordComponentNames() {
      var currentPath = getCurrentPath();
      if (currentPath == null) {
        return Set.of();
      }

      var element = trees.getElement(currentPath);
      if (!(element instanceof TypeElement typeElement)
          || typeElement.getKind() != ElementKind.RECORD) {
        return Set.of();
      }

      var recordComponentNames = new LinkedHashSet<String>();
      for (var recordComponent : typeElement.getRecordComponents()) {
        recordComponentNames.add(recordComponent.getSimpleName().toString());
      }
      return Set.copyOf(recordComponentNames);
    }

    private boolean isInlineSuppressed(ModifiersTree modifiersTree) {
      if (!Boolean.TRUE.equals(config.suppression().inlineEnabled())) {
        return false;
      }

      var classSuppressed = classSuppressionStack.peek() != null && classSuppressionStack.peek();
      if (classSuppressed) {
        return true;
      }

      return InlineSuppression.isSuppressed(
          modifiersTree.getAnnotations(), config.suppression().keys());
    }

    private Visibility resolveVisibility(ModifiersTree modifiersTree) {
      Set<Modifier> flags = modifiersTree.getFlags();
      if (flags.contains(Modifier.PUBLIC)) {
        return Visibility.PUBLIC;
      }
      if (flags.contains(Modifier.PROTECTED)) {
        return Visibility.PROTECTED;
      }
      if (flags.contains(Modifier.PRIVATE)) {
        return Visibility.PRIVATE;
      }
      return Visibility.PACKAGE_PRIVATE;
    }

    private String relativizeSourcePath(CompilationUnitTree tree) {
      try {
        var absolutePath = Path.of(tree.getSourceFile().toUri()).normalize();
        if (absolutePath.startsWith(projectRoot)) {
          return projectRoot.relativize(absolutePath).toString().replace('\\', '/');
        }
        return absolutePath.toString().replace('\\', '/');
      } catch (Exception _) {
        return tree.getSourceFile().getName();
      }
    }
  }
}
