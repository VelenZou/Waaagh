package com.waaagh.cache;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import com.waaagh.entity.HttpMappingInfo;
import com.waaagh.utils.AnnotationParserUtils;
import com.waaagh.utils.ControllerClassScanUtils;
import com.waaagh.utils.FeignClassScanUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;


public class BilateralCacheManager {

  private BilateralCacheManager() {
  }

  // 缓存controller接口数据
  // <projectid, <classpath+methodname, HttpMappingInfo>>
  // 并发容器：查询发生在高亮/导航线程，清理发生在项目关闭或 VFS 变更事件线程
  private static final Map<String, Map<String, HttpMappingInfo>> PROJECT_CONTROLLER_CACHE_MAP = new ConcurrentHashMap<>();

  // 缓存Feign接口数据
  // <projectid, <classpath+methodname, HttpMappingInfo>>
  private static final Map<String, Map<String, HttpMappingInfo>> PROJECT_FEIGN_CACHE_MAP = new ConcurrentHashMap<>();

  // 是否已完成过全量扫描。
  // 注意：缓存的单个方法条目可能来自 setOrCoverXxxCache 的按需补全，
  // 不能把"缓存非空"当作"全量扫描已完成"。
  private static final Set<String> PROJECT_CONTROLLER_FULLY_SCANNED = ConcurrentHashMap.newKeySet();
  private static final Set<String> PROJECT_FEIGN_FULLY_SCANNED = ConcurrentHashMap.newKeySet();

  /**
   * 清除指定项目的所有缓存。
   * 调用方：项目关闭（CacheCleanListener）、源码文件保存/外部修改后（VfsCacheRefreshListener）。
   */
  public static void clear(Project project) {
    String projectId = getProjectId(project);
    PROJECT_CONTROLLER_CACHE_MAP.remove(projectId);
    PROJECT_FEIGN_CACHE_MAP.remove(projectId);
    PROJECT_CONTROLLER_FULLY_SCANNED.remove(projectId);
    PROJECT_FEIGN_FULLY_SCANNED.remove(projectId);
  }

  public static boolean isControllerFullyScanned(Project project) {
    return PROJECT_CONTROLLER_FULLY_SCANNED.contains(getProjectId(project));
  }

  public static void markControllerFullyScanned(Project project) {
    PROJECT_CONTROLLER_FULLY_SCANNED.add(getProjectId(project));
  }

  public static boolean isFeignFullyScanned(Project project) {
    return PROJECT_FEIGN_FULLY_SCANNED.contains(getProjectId(project));
  }

  public static void markFeignFullyScanned(Project project) {
    PROJECT_FEIGN_FULLY_SCANNED.add(getProjectId(project));
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
   * 初始化controller缓存
   */
  public static void initControllerCaches(Project project, List<HttpMappingInfo> controllerCaches) {
    Map<String, HttpMappingInfo> qualifier2Info = PROJECT_CONTROLLER_CACHE_MAP.computeIfAbsent(
        getProjectId(project), k -> new ConcurrentHashMap<>());
    for (HttpMappingInfo controller : controllerCaches) {
      String qualifier = buildKey(controller.getPsiMethod());
      qualifier2Info.put(qualifier, controller);
    }
  }

  /**
   * 初始化feign缓存
   */
  public static void initFeignCaches(Project project, List<HttpMappingInfo> feignCaches) {
    Map<String, HttpMappingInfo> qualifier2Info = PROJECT_FEIGN_CACHE_MAP.computeIfAbsent(
        getProjectId(project), k -> new ConcurrentHashMap<>());
    for (HttpMappingInfo feign : feignCaches) {
      String qualifier = buildKey(feign.getPsiMethod());
      qualifier2Info.put(qualifier, feign);
    }
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

  // -------------------- Key：Qualifier，即 类路径+方法名--------------------

  @NotNull
  private static String buildKey(PsiMethod method) {
    if (method.getContainingClass() == null
        || method.getContainingClass().getQualifiedName() == null) {
      return method.getName(); // 退而求其次
    }
    return method.getContainingClass().getQualifiedName() + method.getName();
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
