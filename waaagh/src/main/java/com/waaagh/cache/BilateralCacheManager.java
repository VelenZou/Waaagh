package com.waaagh.cache;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootModificationTracker;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.waaagh.entity.HttpMappingInfo;
import com.waaagh.utils.AnnotationParserUtils;
import com.waaagh.utils.ControllerClassScanUtils;
import com.waaagh.utils.FeignClassScanUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class BilateralCacheManager {

  private static final Logger LOG = Logger.getInstance(BilateralCacheManager.class);

  /**
   * 缓存时序调试日志开关：启动 IDE 时加 {@code -Dwaaagh.debug=true}。
   * <p>
   * 打开后 idea.log 里会出现 {@code [waaagh-cache]} 前缀的 INFO 行，记录每次
   * 清理 / 重扫的原因、涉及的端点数量与数据版本号——排查"图标 / 映射不刷新"时可直接看时间线。
   */
  private static final boolean DEBUG = Boolean.getBoolean("waaagh.debug");

  private BilateralCacheManager() {
  }

  // 缓存controller接口数据
  // <projectid, <classpath+methodsignature, HttpMappingInfo>>
  // 并发容器：查询发生在高亮/导航线程，清理发生在项目关闭、VFS 变更或 PSI 变更事件线程
  private static final Map<String, Map<String, HttpMappingInfo>> PROJECT_CONTROLLER_CACHE_MAP = new ConcurrentHashMap<>();

  // 缓存Feign接口数据
  // <projectid, <classpath+methodsignature, HttpMappingInfo>>
  private static final Map<String, Map<String, HttpMappingInfo>> PROJECT_FEIGN_CACHE_MAP = new ConcurrentHashMap<>();

  // 是否已完成过全量扫描，值是完成扫描时的"项目根模型版本号"（见 currentRootsStamp）。
  // 注意：缓存的单个方法条目可能来自 setOrCoverXxxCache 的按需补全，
  // 不能把"缓存非空"当作"全量扫描已完成"。
  private static final Map<String, Long> PROJECT_CONTROLLER_FULLY_SCANNED = new ConcurrentHashMap<>();
  private static final Map<String, Long> PROJECT_FEIGN_FULLY_SCANNED = new ConcurrentHashMap<>();

  /**
   * 每个项目的数据版本号：仅在缓存被清理（文件保存 / PSI 编辑停顿 / 外部修改同步 / 索引或结构变化）
   * 或全量扫描完成时 +1。
   * <p>
   * Endpoints 等外部视图订阅它来判断"mapping 数据是否变了"。
   * 不要用全局 PsiModificationTracker：任何 PSI 变化（包括 HTTP Client 面板自身产生的）
   * 都会触发视图重载，形成 重载 → 面板刷新 → PSI 变化 → 重载 的抖动循环。
   */
  private static final Map<String, AtomicLong> PROJECT_DATA_VERSION = new ConcurrentHashMap<>();

  /**
   * 清除指定项目的所有缓存。
   * <p>
   * 调用方：项目关闭（CacheCleanListener）、源码文件保存/外部修改后（VfsCacheRefreshListener）、
   * PSI 编辑停顿后 / 索引完成 / 模块结构变化（ProjectCacheInvalidator）、手动刷新
   * （WaaaghRefreshAction）。
   *
   * @param reason 触发原因，仅在 {@code -Dwaaagh.debug=true} 时写入 idea.log，便于排查刷新时序
   * @return 之前是否真的存在缓存数据（决定要不要通知 Endpoints 视图刷新，避免无意义的空转重扫）
   */
  public static boolean clear(Project project, @NotNull String reason) {
    String projectId = getProjectId(project);
    Map<String, HttpMappingInfo> controllerCaches = PROJECT_CONTROLLER_CACHE_MAP.remove(projectId);
    Map<String, HttpMappingInfo> feignCaches = PROJECT_FEIGN_CACHE_MAP.remove(projectId);
    boolean hadControllerFlag = PROJECT_CONTROLLER_FULLY_SCANNED.remove(projectId) != null;
    boolean hadFeignFlag = PROJECT_FEIGN_FULLY_SCANNED.remove(projectId) != null;
    int controllers = controllerCaches == null ? 0 : controllerCaches.size();
    int feigns = feignCaches == null ? 0 : feignCaches.size();
    if (controllerCaches == null && feignCaches == null && !hadControllerFlag && !hadFeignFlag) {
      log(project, "clear skipped (nothing cached)", reason, controllers, feigns);
      return false;
    }
    bumpDataVersion(project);
    log(project, "cache cleared", reason, controllers, feigns);
    return true;
  }

  /**
   * 项目已彻底关闭：连数据版本号一起移除。
   * <p>
   * 数据版本号在项目生命周期内只增不减；不清理的话，同一个 IDE 会话里反复关闭 / 打开项目会一直残留
   * （一个项目一个 {@link AtomicLong}，量级很小，但没有任何保留价值）。
   */
  public static void forget(Project project) {
    clear(project, "project closed");
    PROJECT_DATA_VERSION.remove(getProjectId(project));
    log(project, "cache forgotten", "project closed", 0, 0);
  }

  /**
   * 当前项目 mapping 数据的修改追踪器（Endpoints provider 使用）。
   */
  public static ModificationTracker getModificationTracker(Project project) {
    AtomicLong version = PROJECT_DATA_VERSION.computeIfAbsent(getProjectId(project),
        key -> new AtomicLong());
    return version::get;
  }

  private static void bumpDataVersion(Project project) {
    PROJECT_DATA_VERSION.computeIfAbsent(getProjectId(project), key -> new AtomicLong())
        .incrementAndGet();
  }

  /**
   * 缓存时序调试日志（{@code -Dwaaagh.debug=true} 时生效）。
   */
  private static void log(@NotNull Project project, @NotNull String action, @NotNull String reason,
      int controllers, int feigns) {
    if (!DEBUG) {
      return;
    }
    AtomicLong version = PROJECT_DATA_VERSION.get(getProjectId(project));
    LOG.info("[waaagh-cache] " + action
        + " | project=" + project.getName()
        + " | reason=" + reason
        + " | controllers=" + controllers + ", feigns=" + feigns
        + " | version=" + (version == null ? 0 : version.get()));
  }

  /**
   * 项目根模型（模块 / 源根 / SDK / 模块依赖）的当前版本号。
   * <p>
   * Maven / Gradle reload、Mark as Sources Root、增删模块等都会改变它。把它记进"全量扫描完成标记"里，
   * 结构变化后 {@link #isControllerFullyScanned(Project)} / {@link #isFeignFullyScanned(Project)}
   * 自动失效（惰性兜底）。正常情况下 ProjectCacheInvalidator 会通过 ModuleRootListener 先做主动清理，
   * 这里防御监听器缺失或漏事件的场景。
   * <p>
   * 只在项目根结构变化时变化，不包含 PSI 编辑计数——PSI 编辑由 ProjectCacheInvalidator 防抖清理，
   * 不能在这里逐次比较，否则每次按键都会触发全量重扫。
   */
  private static long currentRootsStamp(@NotNull Project project) {
    if (project.isDisposed()) {
      return Long.MIN_VALUE;
    }
    return ProjectRootModificationTracker.getInstance(project).getModificationCount();
  }

  public static boolean isControllerFullyScanned(Project project) {
    Long scannedStamp = PROJECT_CONTROLLER_FULLY_SCANNED.get(getProjectId(project));
    return scannedStamp != null && scannedStamp.longValue() == currentRootsStamp(project);
  }

  public static void markControllerFullyScanned(Project project) {
    PROJECT_CONTROLLER_FULLY_SCANNED.put(getProjectId(project), currentRootsStamp(project));
  }

  public static boolean isFeignFullyScanned(Project project) {
    Long scannedStamp = PROJECT_FEIGN_FULLY_SCANNED.get(getProjectId(project));
    return scannedStamp != null && scannedStamp.longValue() == currentRootsStamp(project);
  }

  public static void markFeignFullyScanned(Project project) {
    PROJECT_FEIGN_FULLY_SCANNED.put(getProjectId(project), currentRootsStamp(project));
  }

  /**
   * 获取所有的controller缓存
   */
  public static Map<String, HttpMappingInfo> queryControllerCaches(Project project) {
    return PROJECT_CONTROLLER_CACHE_MAP.get(getProjectId(project));
  }

  /**
   * 获取所有的feign缓存
   */
  public static Map<String, HttpMappingInfo> queryFeignCaches(Project project) {
    return PROJECT_FEIGN_CACHE_MAP.get(getProjectId(project));
  }

  /**
   * 当前有效的 controller 缓存条目快照。
   * <p>
   * 缓存里的 {@link PsiMethod} 可能随 PSI 重建失效（项目重新导入、Invalidate Caches 等），
   * 这类条目不应再参与匹配与导航，直接过滤掉。
   */
  public static List<HttpMappingInfo> usableControllerCaches(Project project) {
    return usableCaches(queryControllerCaches(project));
  }

  /**
   * 当前有效的 feign 缓存条目快照（过滤失效 PSI）。
   */
  public static List<HttpMappingInfo> usableFeignCaches(Project project) {
    return usableCaches(queryFeignCaches(project));
  }

  private static List<HttpMappingInfo> usableCaches(@Nullable Map<String, HttpMappingInfo> caches) {
    if (caches == null || caches.isEmpty()) {
      return Collections.emptyList();
    }
    List<HttpMappingInfo> usable = new ArrayList<>(caches.size());
    for (HttpMappingInfo info : caches.values()) {
      PsiMethod method = info.getPsiMethod();
      if (method != null && method.isValid()) {
        usable.add(info);
      }
    }
    return usable;
  }

  /**
   * 缓存中是否存在属于该文件的端点条目。
   * <p>
   * 用于"这个文件的变化要不要清缓存"的判定：即使文件当前已经没有任何映射注解，
   * 只要缓存里还留着它声明的端点（例如刚删掉方法 / 注解，或改了路径常量），就必须重扫。
   */
  public static boolean hasCachedEntriesIn(@NotNull PsiFile file) {
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) {
      return true; // 拿不到文件对象：保守认为相关
    }
    String projectId = getProjectId(file.getProject());
    return containsFile(PROJECT_CONTROLLER_CACHE_MAP.get(projectId), virtualFile)
        || containsFile(PROJECT_FEIGN_CACHE_MAP.get(projectId), virtualFile);
  }

  private static boolean containsFile(@Nullable Map<String, HttpMappingInfo> caches,
      @NotNull VirtualFile file) {
    if (caches == null) {
      return false;
    }
    for (HttpMappingInfo info : caches.values()) {
      PsiMethod method = info.getPsiMethod();
      if (method == null || !method.isValid()) {
        continue;
      }
      PsiFile containingFile = method.getContainingFile();
      if (containingFile != null && file.equals(containingFile.getVirtualFile())) {
        return true;
      }
    }
    return false;
  }

  /**
   * 初始化controller缓存（全量快照）。
   * <p>
   * 在现有表上 clear + 重填，而不是往旧表里合并：重扫可能发生在没有先 {@link #clear(Project)}
   * 的路径上（例如模块根版本号兜底失效），合并会让上一次扫描遗留的条目继续参与匹配。
   * 保留表对象本身（不整表替换），避免查询线程拿到已被丢弃的旧引用。
   */
  public static void initControllerCaches(Project project, List<HttpMappingInfo> controllerCaches) {
    Map<String, HttpMappingInfo> qualifier2Info = PROJECT_CONTROLLER_CACHE_MAP.computeIfAbsent(
        getProjectId(project), k -> new ConcurrentHashMap<>());
    qualifier2Info.clear();
    for (HttpMappingInfo controller : controllerCaches) {
      qualifier2Info.put(buildKey(controller.getPsiMethod()), controller);
    }
    bumpDataVersion(project);
    log(project, "scanned controllers", "full scan", qualifier2Info.size(), 0);
  }

  /**
   * 初始化feign缓存（全量快照，语义见 {@link #initControllerCaches(Project, List)}）
   */
  public static void initFeignCaches(Project project, List<HttpMappingInfo> feignCaches) {
    Map<String, HttpMappingInfo> qualifier2Info = PROJECT_FEIGN_CACHE_MAP.computeIfAbsent(
        getProjectId(project), k -> new ConcurrentHashMap<>());
    qualifier2Info.clear();
    for (HttpMappingInfo feign : feignCaches) {
      qualifier2Info.put(buildKey(feign.getPsiMethod()), feign);
    }
    bumpDataVersion(project);
    log(project, "scanned feigns", "full scan", 0, qualifier2Info.size());
  }

  /**
   * 设置当前的feign方法的缓存,注意防止NPE
   */
  public static HttpMappingInfo setFeignCache(PsiMethod feignMethod) {
    Project project = feignMethod.getProject();

    //此时有可能先于feign全扫描，所以feign缓存有可能为空
    Map<String, HttpMappingInfo> qualifier2Info = PROJECT_FEIGN_CACHE_MAP.computeIfAbsent(
        getProjectId(project), k -> new ConcurrentHashMap<>());
    String qualifier = buildKey(feignMethod);
    HttpMappingInfo feignInfo = null;
    //在用户打注释/***/期间，psiMethod会有一瞬间不再拥有注解，此时HttpMappingInfo.of将返回为空, 注意避免HashMap的value为空的情况
    if (Objects.nonNull(
        feignInfo = FeignClassScanUtils.feignOfPsiMethod(feignMethod.getContainingClass(),
            feignMethod))) {
      qualifier2Info.put(qualifier, feignInfo);
    }
    return feignInfo;
  }

  /**
   * 获取或者设置某个feign方法的缓存
   */
  public static HttpMappingInfo getOrSetFeignCache(PsiMethod feignMethod) {
    if (!AnnotationParserUtils.containsRestfulAnnotation(feignMethod)) {
      return null;
    }
    Map<String, HttpMappingInfo> qualifier2Info = PROJECT_FEIGN_CACHE_MAP.computeIfAbsent(
        getProjectId(feignMethod.getProject()), k -> new ConcurrentHashMap<>());
    String qualifier = buildKey(feignMethod);
    if (Objects.isNull(qualifier2Info.get(qualifier))) {
      setFeignCache(feignMethod);
    }
    return qualifier2Info.get(qualifier);
  }

  /**
   * 获取或者设置某个controller方法的缓存
   */
  public static HttpMappingInfo getOrSetControllerCache(PsiMethod controllerMethod) {
    if (!AnnotationParserUtils.containsRestfulAnnotation(controllerMethod)) {
      return null;
    }
    Map<String, HttpMappingInfo> qualifier2Info = PROJECT_CONTROLLER_CACHE_MAP.computeIfAbsent(
        getProjectId(controllerMethod.getProject()), k -> new ConcurrentHashMap<>());
    String qualifier = buildKey(controllerMethod);
    if (Objects.isNull(qualifier2Info.get(qualifier))) {
      setControllerCache(controllerMethod);
    }
    return qualifier2Info.get(qualifier);
  }

  /**
   * 设置当前的controller方法的接口缓存,注意防止NPE
   */
  public static HttpMappingInfo setControllerCache(PsiMethod controllerMethod) {
    Project project = controllerMethod.getProject();

    // 此时有可能先于controller全扫描，所以controller缓存有可能为空
    Map<String, HttpMappingInfo> qualifier2Info = PROJECT_CONTROLLER_CACHE_MAP.computeIfAbsent(
        getProjectId(project), k -> new ConcurrentHashMap<>());
    String qualifier = buildKey(controllerMethod);
    // 在用户打注释/***/期间，psiMethod会有一瞬间不再拥有注解，此时HttpMappingInfo.of将返回为空, 注意避免HashMap的value为空的情况
    HttpMappingInfo controllerInfo = null;
    if (Objects.nonNull(controllerInfo = ControllerClassScanUtils.controllerOfPsiMethod(
        controllerMethod.getContainingClass(), project, controllerMethod))) {
      qualifier2Info.put(qualifier, controllerInfo);
    }
    return controllerInfo;
  }

  // -------------------- Key：Qualifier，即 类路径+方法签名 --------------------
  //
  // 必须带上参数类型：同名重载方法（如 find(Long) / find(String)）如果只按
  // "类全名 + 方法名"建 key 会互相覆盖，导致部分端点从缓存里消失。

  @NotNull
  private static String buildKey(PsiMethod method) {
    if (method.getContainingClass() == null
        || method.getContainingClass().getQualifiedName() == null) {
      return method.getName(); // 退而求其次
    }
    StringBuilder key = new StringBuilder(method.getContainingClass().getQualifiedName())
        .append('#')
        .append(method.getName())
        .append('(');
    for (PsiParameter parameter : method.getParameterList().getParameters()) {
      key.append(parameter.getType().getCanonicalText()).append(',');
    }
    return key.append(')').toString();
  }

  @NotNull
  private static String getProjectId(Project project) {
    String path = project.getBasePath();
    return path != null ? path : String.valueOf(project.hashCode());
  }

  /**
   * 为了支持用户对当前feign接口更新，无论缓存是否存在，设置或者覆盖缓存
   */
  public static HttpMappingInfo setOrCoverFeignCache(PsiMethod psiMethod) {
    return setFeignCache(psiMethod);
  }

  /**
   * 为了支持用户对当前controller接口更新，无论缓存是否存在，设置或者覆盖缓存
   */
  public static HttpMappingInfo setOrCoverControllerCache(PsiMethod psiMethod) {
    return setControllerCache(psiMethod);
  }
}
