package com.waaagh.utils;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiBinaryExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParenthesizedExpression;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.PsiPrefixExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import com.waaagh.properties.ServerParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

/**
 * 解析 Spring MVC “按子包自动加路径前缀”的配置：
 * {@code WebMvcConfigurer#configurePathMatch(PathMatchConfigurer)} 里的
 * {@code configurer.addPathPrefix(prefix, predicate)} 调用。
 * <p>
 * Spring 语义（Spring 6 {@code RequestMappingHandlerMapping#getPathPrefix}）：
 * 按注册顺序取<b>第一个</b>命中的前缀，拼在类路径之前，即
 * {@code server.servlet.context-path + spring.mvc.servlet.path + 前缀 + 类 @RequestMapping + 方法路径}。
 * <p>
 * 支持的 predicate 写法：
 * <ul>
 *   <li>lambda：{@code c -> c.getPackageName().startsWith("x")}，支持 {@code &&} / {@code ||} / {@code !}
 *       及 {@code getName()} / {@code getSimpleName()} / {@code getPackage().getName()}；</li>
 *   <li>lambda：{@code c -> c.isAnnotationPresent(X.class)}（含组合注解的深度查找）；</li>
 *   <li>{@code HandlerTypePredicate.forBasePackage / forBasePackageClass / forAnnotation /
 *       forAssignableType / forAnyHandlerType}；</li>
 *   <li>以上谓词的 {@code .and(...)} / {@code .or(...)} / {@code .negate()} 组合；</li>
 *   <li>常量 / 静态字段 / 局部变量引用（递归解析初始化表达式，含字符串 {@code +} 拼接与
 *       {@code @Value("${key}")}）。</li>
 * </ul>
 * 解析不了的规则会被跳过（宁可漏加前缀，也不加错）。
 */
public final class PathPrefixResolver {

  private static final String PATH_MATCH_CONFIGURER =
      "org.springframework.web.servlet.config.annotation.PathMatchConfigurer";
  private static final String HANDLER_TYPE_PREDICATE =
      "org.springframework.web.method.HandlerTypePredicate";
  private static final String VALUE_ANNOTATION =
      "org.springframework.beans.factory.annotation.Value";
  private static final String ADD_PATH_PREFIX_METHOD = "addPathPrefix";

  private static final Set<String> STRING_MATCH_METHODS =
      new HashSet<>(Arrays.asList("startsWith", "endsWith", "contains", "equals", "matches"));

  private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");
  private static final int MAX_RESOLVE_DEPTH = 8;

  private static final Key<CachedValue<List<PrefixEntry>>> PREFIX_ENTRIES_KEY =
      Key.create("waaagh.path.prefix.entries");

  private PathPrefixResolver() {
  }

  /**
   * 计算 Controller 类命中的路径前缀：按注册顺序取第一个命中的规则，
   * 解析 {@code ${...}} 占位符并做首尾斜杠归一化；没有命中返回空字符串。
   */
  public static String resolveControllerPathPrefix(PsiClass controllerClass, Project project) {
    if (controllerClass == null || !controllerClass.isValid() || DumbService.isDumb(project)) {
      return "";
    }
    for (PrefixEntry entry : entriesOf(project)) {
      if (entry.predicate.test(controllerClass)) {
        String resolved = resolvePlaceholders(entry.prefix, controllerClass, project);
        String normalized = normalizePrefix(resolved);
        return normalized == null ? "" : normalized;
      }
    }
    return "";
  }

  // -------------------- 扫描项目里的 addPathPrefix 调用 --------------------

  private static List<PrefixEntry> entriesOf(Project project) {
    return CachedValuesManager.getManager(project).getCachedValue(project, PREFIX_ENTRIES_KEY,
        () -> CachedValueProvider.Result.create(scanEntries(project),
            PsiModificationTracker.MODIFICATION_COUNT),
        false);
  }

  private static List<PrefixEntry> scanEntries(Project project) {
    if (DumbService.isDumb(project)) {
      return Collections.emptyList();
    }
    List<PrefixEntry> entries = new ArrayList<>();
    PsiSearchHelper.getInstance(project).processElementsWithWord(
        (element, offsetInElement) -> {
          collectEntry(element, entries);
          return true;
        },
        GlobalSearchScope.projectScope(project),
        ADD_PATH_PREFIX_METHOD,
        UsageSearchContext.IN_CODE,
        true);
    // 近似 Spring 的注册顺序：同文件按源码顺序，跨文件按文件路径排序
    entries.sort(Comparator
        .comparing((PrefixEntry entry) -> filePathOf(entry.anchor))
        .thenComparingInt(entry -> entry.anchor.getTextOffset()));
    return entries;
  }

  private static void collectEntry(PsiElement word, List<PrefixEntry> entries) {
    PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(word,
        PsiMethodCallExpression.class, false);
    if (call == null || !ADD_PATH_PREFIX_METHOD.equals(
        call.getMethodExpression().getReferenceName())) {
      return;
    }
    // 只认项目源码里的配置，排除依赖/生成代码
    if (!ProjectUtils.isBizElement(call)) {
      return;
    }
    PsiMethod method = call.resolveMethod();
    PsiClass containingClass = method == null ? null : method.getContainingClass();
    if (containingClass == null || !PATH_MATCH_CONFIGURER.equals(containingClass.getQualifiedName())) {
      return;
    }
    PsiExpression[] arguments = call.getArgumentList().getExpressions();
    if (arguments.length < 2) {
      return;
    }
    String prefix = resolveStringValue(arguments[0]);
    if (StringUtils.isBlank(prefix)) {
      return;
    }
    ClassPredicate predicate = parsePredicate(arguments[1], 0);
    if (predicate == null) {
      return;
    }
    entries.add(new PrefixEntry(prefix, predicate, call));
  }

  private static String filePathOf(PsiMethodCallExpression call) {
    VirtualFile virtualFile = call.getContainingFile().getVirtualFile();
    return virtualFile == null ? "" : virtualFile.getPath();
  }

  private static final class PrefixEntry {

    private final String prefix;
    private final ClassPredicate predicate;
    private final PsiMethodCallExpression anchor;

    private PrefixEntry(String prefix, ClassPredicate predicate, PsiMethodCallExpression anchor) {
      this.prefix = prefix;
      this.predicate = predicate;
      this.anchor = anchor;
    }
  }

  @FunctionalInterface
  private interface ClassPredicate {

    boolean test(PsiClass controllerClass);
  }

  // -------------------- 前缀占位符 / 归一化 --------------------

  @Nullable
  private static String resolvePlaceholders(String rawPrefix, PsiClass controllerClass,
      Project project) {
    if (!rawPrefix.contains("${")) {
      return rawPrefix;
    }
    Optional<PsiDirectory> resourcesDirectory =
        ServerParser.getServiceModuleResourcesDirectory(controllerClass, project);
    if (!resourcesDirectory.isPresent()) {
      return null;
    }
    Matcher matcher = PLACEHOLDER.matcher(rawPrefix);
    StringBuffer resolved = new StringBuffer();
    while (matcher.find()) {
      String value = lookupConfigValue(resourcesDirectory.get(), matcher.group(1));
      if (StringUtils.isEmpty(value)) {
        return null; // 占位符解析不了就不加前缀
      }
      matcher.appendReplacement(resolved, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(resolved);
    return resolved.toString();
  }

  private static String lookupConfigValue(PsiDirectory resourcesDirectory, String placeholder) {
    String key = placeholder;
    String fallback = null;
    int colon = placeholder.indexOf(':');
    if (colon >= 0) {
      key = placeholder.substring(0, colon);
      fallback = placeholder.substring(colon + 1);
    }
    String value = ControllerClassScanUtils.extractSpringProperties(resourcesDirectory, key);
    return StringUtils.isEmpty(value) ? fallback : value;
  }

  @Nullable
  private static String normalizePrefix(@Nullable String prefix) {
    if (prefix == null) {
      return null;
    }
    String normalized = prefix.trim();
    if (normalized.isEmpty()) {
      return null;
    }
    if (!normalized.startsWith("/")) {
      normalized = "/" + normalized;
    }
    while (normalized.length() > 1 && normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return "/".equals(normalized) ? "" : normalized;
  }

  // -------------------- predicate 解析 --------------------

  @Nullable
  private static ClassPredicate parsePredicate(@Nullable PsiExpression expression, int depth) {
    if (expression == null || depth > MAX_RESOLVE_DEPTH) {
      return null;
    }
    if (expression instanceof PsiParenthesizedExpression) {
      return parsePredicate(((PsiParenthesizedExpression) expression).getExpression(), depth);
    }
    if (expression instanceof PsiLambdaExpression) {
      return parseLambda((PsiLambdaExpression) expression, depth);
    }
    if (expression instanceof PsiMethodCallExpression) {
      return parsePredicateCall((PsiMethodCallExpression) expression, depth);
    }
    if (expression instanceof PsiReferenceExpression) {
      // 常量 / 字段 / 局部变量：递归解析初始化表达式
      PsiElement resolved = ((PsiReferenceExpression) expression).resolve();
      PsiExpression initializer = initializerOf(resolved);
      if (initializer != null) {
        return parsePredicate(initializer, depth + 1);
      }
    }
    return null;
  }

  @Nullable
  private static ClassPredicate parsePredicateCall(PsiMethodCallExpression call, int depth) {
    PsiMethod method = call.resolveMethod();
    PsiClass containingClass = method == null ? null : method.getContainingClass();
    String containingName = containingClass == null ? null : containingClass.getQualifiedName();
    String name = method != null ? method.getName() : call.getMethodExpression().getReferenceName();
    if (name == null) {
      return null;
    }
    if (HANDLER_TYPE_PREDICATE.equals(containingName)) {
      return parseHandlerTypePredicate(call, name);
    }
    // Predicate#and / #or / #negate 组合
    if ("and".equals(name) || "or".equals(name) || "negate".equals(name)) {
      ClassPredicate left = parsePredicate(call.getMethodExpression().getQualifierExpression(),
          depth + 1);
      if (left == null) {
        return null;
      }
      if ("negate".equals(name)) {
        return controllerClass -> !left.test(controllerClass);
      }
      PsiExpression[] arguments = call.getArgumentList().getExpressions();
      ClassPredicate right = arguments.length == 1 ? parsePredicate(arguments[0], depth + 1) : null;
      if (right == null) {
        return null;
      }
      if ("and".equals(name)) {
        return controllerClass -> left.test(controllerClass) && right.test(controllerClass);
      }
      return controllerClass -> left.test(controllerClass) || right.test(controllerClass);
    }
    return null;
  }

  @Nullable
  private static ClassPredicate parseHandlerTypePredicate(PsiMethodCallExpression call,
      String methodName) {
    PsiExpression[] arguments = call.getArgumentList().getExpressions();
    switch (methodName) {
      case "forAnyHandlerType":
        return controllerClass -> true;
      case "forBasePackage": {
        List<String> basePackages = new ArrayList<>();
        for (PsiExpression argument : arguments) {
          String packageName = resolveStringValue(argument);
          if (StringUtils.isBlank(packageName)) {
            return null;
          }
          basePackages.add(packageName);
        }
        return controllerClass -> matchesBasePackage(controllerClass, basePackages);
      }
      case "forBasePackageClass": {
        List<String> basePackages = new ArrayList<>();
        for (PsiExpression argument : arguments) {
          PsiClass packageClass = resolveClassAccess(argument);
          if (packageClass == null) {
            return null;
          }
          String packageName = packageNameOf(packageClass);
          if (StringUtils.isBlank(packageName)) {
            return null;
          }
          basePackages.add(packageName);
        }
        return controllerClass -> matchesBasePackage(controllerClass, basePackages);
      }
      case "forAnnotation": {
        List<String> annotationNames = new ArrayList<>();
        for (PsiExpression argument : arguments) {
          PsiClass annotationClass = resolveClassAccess(argument);
          if (annotationClass == null || annotationClass.getQualifiedName() == null) {
            return null;
          }
          annotationNames.add(annotationClass.getQualifiedName());
        }
        return controllerClass -> {
          for (String annotationName : annotationNames) {
            if (hasAnnotationDeep(controllerClass, annotationName, new HashSet<>())) {
              return true;
            }
          }
          return false;
        };
      }
      case "forAssignableType": {
        List<String> typeNames = new ArrayList<>();
        for (PsiExpression argument : arguments) {
          PsiClass typeClass = resolveClassAccess(argument);
          if (typeClass == null || typeClass.getQualifiedName() == null) {
            return null;
          }
          typeNames.add(typeClass.getQualifiedName());
        }
        return controllerClass -> {
          for (String typeName : typeNames) {
            if (isAssignableTo(controllerClass, typeName)) {
              return true;
            }
          }
          return false;
        };
      }
      default:
        return null;
    }
  }

  private static boolean matchesBasePackage(PsiClass controllerClass, List<String> basePackages) {
    String className = qualifiedNameOf(controllerClass);
    for (String basePackage : basePackages) {
      // HandlerTypePredicate 会把包名规范为 "x.y." 再与类全名做 startsWith
      String packagePrefix = basePackage.endsWith(".") ? basePackage : basePackage + ".";
      if (className.startsWith(packagePrefix)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isAssignableTo(PsiClass controllerClass, String superTypeName) {
    if (superTypeName.equals(controllerClass.getQualifiedName())) {
      return true;
    }
    PsiClass superType = JavaPsiFacade.getInstance(controllerClass.getProject())
        .findClass(superTypeName, GlobalSearchScope.allScope(controllerClass.getProject()));
    return superType != null && controllerClass.isInheritor(superType, true);
  }

  private static boolean hasAnnotationDeep(PsiClass psiClass, String annotationName,
      Set<String> visited) {
    if (psiClass == null) {
      return false;
    }
    String className = psiClass.getQualifiedName();
    if (className == null || !visited.add(className)) {
      return false;
    }
    Project project = psiClass.getProject();
    JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
    for (PsiAnnotation annotation : psiClass.getAnnotations()) {
      String name = annotation.getQualifiedName();
      if (annotationName.equals(name)) {
        return true;
      }
      if (name == null) {
        continue;
      }
      PsiClass annotationClass = facade.findClass(name, GlobalSearchScope.allScope(project));
      if (hasAnnotationDeep(annotationClass, annotationName, visited)) {
        return true;
      }
    }
    return false;
  }

  // -------------------- lambda 布尔表达式解析 --------------------

  @Nullable
  private static ClassPredicate parseLambda(PsiLambdaExpression lambda, int depth) {
    PsiParameter[] parameters = lambda.getParameterList().getParameters();
    if (parameters.length != 1) {
      return null;
    }
    PsiExpression body = lambdaBodyOf(lambda);
    if (body == null) {
      return null;
    }
    return parseBooleanExpression(body, parameters[0], depth);
  }

  @Nullable
  private static PsiExpression lambdaBodyOf(PsiLambdaExpression lambda) {
    PsiElement body = lambda.getBody();
    if (body instanceof PsiExpression) {
      return (PsiExpression) body;
    }
    if (body instanceof PsiCodeBlock) {
      PsiStatement[] statements = ((PsiCodeBlock) body).getStatements();
      if (statements.length == 1 && statements[0] instanceof PsiReturnStatement) {
        return ((PsiReturnStatement) statements[0]).getReturnValue();
      }
    }
    return null;
  }

  @Nullable
  private static ClassPredicate parseBooleanExpression(@Nullable PsiExpression expression,
      PsiParameter parameter, int depth) {
    if (expression == null || depth > MAX_RESOLVE_DEPTH) {
      return null;
    }
    if (expression instanceof PsiParenthesizedExpression) {
      return parseBooleanExpression(((PsiParenthesizedExpression) expression).getExpression(),
          parameter, depth);
    }
    if (expression instanceof PsiPrefixExpression) {
      PsiPrefixExpression prefixExpression = (PsiPrefixExpression) expression;
      if (JavaTokenType.EXCL.equals(prefixExpression.getOperationTokenType())) {
        ClassPredicate inner = parseBooleanExpression(prefixExpression.getOperand(), parameter,
            depth);
        return inner == null ? null : controllerClass -> !inner.test(controllerClass);
      }
      return null;
    }
    if (expression instanceof PsiBinaryExpression) {
      PsiBinaryExpression binary = (PsiBinaryExpression) expression;
      IElementType operation = binary.getOperationTokenType();
      if (JavaTokenType.ANDAND.equals(operation) || JavaTokenType.OROR.equals(operation)) {
        ClassPredicate left = parseBooleanExpression(binary.getLOperand(), parameter, depth);
        ClassPredicate right = parseBooleanExpression(binary.getROperand(), parameter, depth);
        if (left == null || right == null) {
          return null;
        }
        if (JavaTokenType.ANDAND.equals(operation)) {
          return controllerClass -> left.test(controllerClass) && right.test(controllerClass);
        }
        return controllerClass -> left.test(controllerClass) || right.test(controllerClass);
      }
      return null;
    }
    if (expression instanceof PsiLiteralExpression) {
      Object value = ((PsiLiteralExpression) expression).getValue();
      if (Boolean.TRUE.equals(value)) {
        return controllerClass -> true;
      }
      if (Boolean.FALSE.equals(value)) {
        return controllerClass -> false;
      }
      return null;
    }
    if (expression instanceof PsiMethodCallExpression) {
      return parseBooleanCall((PsiMethodCallExpression) expression, parameter, depth);
    }
    return null;
  }

  @Nullable
  private static ClassPredicate parseBooleanCall(PsiMethodCallExpression call,
      PsiParameter parameter, int depth) {
    String name = call.getMethodExpression().getReferenceName();
    if (name == null) {
      return null;
    }
    PsiExpression[] arguments = call.getArgumentList().getExpressions();
    PsiExpression qualifier = call.getMethodExpression().getQualifierExpression();

    // c.isAnnotationPresent(X.class)
    if ("isAnnotationPresent".equals(name) && arguments.length == 1
        && isParameterReference(qualifier, parameter)) {
      PsiClass annotationClass = resolveClassAccess(arguments[0]);
      if (annotationClass == null || annotationClass.getQualifiedName() == null) {
        return null;
      }
      String annotationName = annotationClass.getQualifiedName();
      return controllerClass -> hasAnnotationDeep(controllerClass, annotationName, new HashSet<>());
    }

    // c.getPackageName().startsWith("...") 等字符串判断
    if (arguments.length == 1 && STRING_MATCH_METHODS.contains(name)) {
      Function<PsiClass, String> producer = parseStringProducer(qualifier, parameter, depth);
      String expected = resolveStringValue(arguments[0]);
      if (producer == null || expected == null) {
        return null;
      }
      switch (name) {
        case "startsWith":
          return controllerClass -> stringOf(producer, controllerClass).startsWith(expected);
        case "endsWith":
          return controllerClass -> stringOf(producer, controllerClass).endsWith(expected);
        case "contains":
          return controllerClass -> stringOf(producer, controllerClass).contains(expected);
        case "equals":
          return controllerClass -> stringOf(producer, controllerClass).equals(expected);
        case "matches":
          Pattern pattern;
          try {
            pattern = Pattern.compile(expected);
          } catch (RuntimeException e) {
            return null;
          }
          return controllerClass -> pattern.matcher(stringOf(producer, controllerClass)).matches();
        default:
          return null;
      }
    }
    return null;
  }

  private static String stringOf(Function<PsiClass, String> producer, PsiClass controllerClass) {
    String value = producer.apply(controllerClass);
    return value == null ? "" : value;
  }

  @Nullable
  private static Function<PsiClass, String> parseStringProducer(@Nullable PsiExpression expression,
      PsiParameter parameter, int depth) {
    if (expression == null || depth > MAX_RESOLVE_DEPTH) {
      return null;
    }
    if (expression instanceof PsiParenthesizedExpression) {
      return parseStringProducer(((PsiParenthesizedExpression) expression).getExpression(),
          parameter, depth);
    }
    if (!(expression instanceof PsiMethodCallExpression)) {
      return null;
    }
    PsiMethodCallExpression call = (PsiMethodCallExpression) expression;
    String name = call.getMethodExpression().getReferenceName();
    PsiExpression qualifier = call.getMethodExpression().getQualifierExpression();
    if (name == null) {
      return null;
    }
    if (isParameterReference(qualifier, parameter)) {
      switch (name) {
        case "getPackageName":
          return PathPrefixResolver::packageNameOf;
        case "getName":
          return PathPrefixResolver::qualifiedNameOf;
        case "getSimpleName":
          return PsiClass::getName;
        default:
          return null;
      }
    }
    // c.getPackage().getName()
    if ("getName".equals(name) && qualifier instanceof PsiMethodCallExpression) {
      PsiMethodCallExpression innerCall = (PsiMethodCallExpression) qualifier;
      if ("getPackage".equals(innerCall.getMethodExpression().getReferenceName())
          && isParameterReference(innerCall.getMethodExpression().getQualifierExpression(),
          parameter)) {
        return PathPrefixResolver::packageNameOf;
      }
    }
    return null;
  }

  private static boolean isParameterReference(@Nullable PsiExpression expression,
      PsiParameter parameter) {
    if (!(expression instanceof PsiReferenceExpression)) {
      return false;
    }
    PsiElement resolved = ((PsiReferenceExpression) expression).resolve();
    return resolved != null && resolved.equals(parameter);
  }

  // -------------------- 字符串 / 类字面量解析 --------------------

  @Nullable
  private static String resolveStringValue(@Nullable PsiExpression expression) {
    return resolveStringValue(expression, 0);
  }

  @Nullable
  private static String resolveStringValue(@Nullable PsiExpression expression, int depth) {
    if (expression == null || depth > MAX_RESOLVE_DEPTH) {
      return null;
    }
    if (expression instanceof PsiLiteralExpression) {
      Object value = ((PsiLiteralExpression) expression).getValue();
      return value instanceof String ? (String) value : null;
    }
    if (expression instanceof PsiParenthesizedExpression) {
      return resolveStringValue(((PsiParenthesizedExpression) expression).getExpression(), depth);
    }
    if (expression instanceof PsiPolyadicExpression) {
      PsiPolyadicExpression polyadic = (PsiPolyadicExpression) expression;
      if (!JavaTokenType.PLUS.equals(polyadic.getOperationTokenType())) {
        return null;
      }
      StringBuilder builder = new StringBuilder();
      for (PsiExpression operand : polyadic.getOperands()) {
        String part = resolveStringValue(operand, depth + 1);
        if (part == null) {
          return null;
        }
        builder.append(part);
      }
      return builder.toString();
    }
    if (expression instanceof PsiReferenceExpression) {
      PsiElement resolved = ((PsiReferenceExpression) expression).resolve();
      if (resolved instanceof PsiField) {
        String value = resolveValueAnnotation((PsiField) resolved);
        if (value != null) {
          return value;
        }
      }
      return resolveStringValue(initializerOf(resolved), depth + 1);
    }
    return null;
  }

  /**
   * 解析 {@code @Value("${key:default}")} 字段：返回原始占位符字符串，稍后按模块配置解析。
   */
  @Nullable
  private static String resolveValueAnnotation(PsiField field) {
    PsiAnnotation annotation = field.getAnnotation(VALUE_ANNOTATION);
    if (annotation == null) {
      return null;
    }
    PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
    if (value instanceof PsiLiteralExpression) {
      Object literal = ((PsiLiteralExpression) value).getValue();
      return literal instanceof String ? (String) literal : null;
    }
    return null;
  }

  @Nullable
  private static PsiExpression initializerOf(@Nullable PsiElement element) {
    if (element instanceof PsiField) {
      return ((PsiField) element).getInitializer();
    }
    if (element instanceof PsiLocalVariable) {
      return ((PsiLocalVariable) element).getInitializer();
    }
    return null;
  }

  @Nullable
  private static PsiClass resolveClassAccess(@Nullable PsiExpression expression) {
    if (!(expression instanceof PsiClassObjectAccessExpression)) {
      return null;
    }
    PsiType type = ((PsiClassObjectAccessExpression) expression).getOperand().getType();
    if (type instanceof PsiClassType) {
      return ((PsiClassType) type).resolve();
    }
    return null;
  }

  private static String packageNameOf(PsiClass psiClass) {
    PsiFile file = psiClass.getContainingFile();
    if (file instanceof PsiJavaFile) {
      String packageName = ((PsiJavaFile) file).getPackageName();
      return packageName == null ? "" : packageName;
    }
    String qualifiedName = psiClass.getQualifiedName();
    if (qualifiedName == null) {
      return "";
    }
    int lastDot = qualifiedName.lastIndexOf('.');
    return lastDot < 0 ? "" : qualifiedName.substring(0, lastDot);
  }

  private static String qualifiedNameOf(PsiClass psiClass) {
    String qualifiedName = psiClass.getQualifiedName();
    return qualifiedName == null ? "" : qualifiedName;
  }
}
