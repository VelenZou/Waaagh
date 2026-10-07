package com.waaagh.utils;

import static com.waaagh.enums.SpringBootMethodAnnotation.REQUEST_MAPPING;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPackage;
import com.waaagh.cache.BilateralCacheManager;
import com.waaagh.cache.InitialPsiClassCacheManager;
import com.waaagh.entity.HttpMappingInfo;
import com.waaagh.properties.ConfigReader;
import com.waaagh.properties.ServerParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

public class ControllerClassScanUtils {

  private static final String SPRINGBOOT_SERVER_PATH = "server.servlet.context-path";
  private static final String SPRINGMVC_PATH = "spring.mvc.servlet.path";
  // 初始化PsiClass缓存管理器
  private static final InitialPsiClassCacheManager initialPsiClassCacheManager = InitialPsiClassCacheManager.getInstance();

  private ControllerClassScanUtils() {
  }

  /**
   * 全量扫描工程中的controllerinfos
   */
  public static List<HttpMappingInfo> scanControllerPaths(Project project) {
    // 检查是否在 Dumb 模式下，以避免在项目构建期间执行代码
    if (DumbService.isDumb(project)) {
      return Collections.emptyList();
    }

    PsiManager psiManager = PsiManager.getInstance(project);

    PsiPackage rootPackage = JavaPsiFacade.getInstance(psiManager.getProject()).findPackage("");

    // 获取项目中的所有源文件
    List<HttpMappingInfo> httpMappingInfos = new ArrayList<>();

    // 获取项目ID
    String projectId = project.getBasePath();

    List<PsiClass> javaFiles = initialPsiClassCacheManager.queryCurProjectPsiClassesCache(
        projectId);

    if (CollectionUtils.isEmpty(javaFiles)) {
      javaFiles = ProjectUtils.scanProjectCls(rootPackage, project);
      initialPsiClassCacheManager.initCurProjectPsiClassCache(projectId, javaFiles);
    }
    // controller接口缓存查询
    Map<String, HttpMappingInfo> controllerCaches = BilateralCacheManager.queryControllerCaches(
        project);

    if (MapUtils.isNotEmpty(controllerCaches)) {
      return new ArrayList<>(controllerCaches.values());
    }
    // 创建全部的controller信息
    for (PsiClass psiClass : javaFiles) {
      // 校验 psiClass 的有效性，毕竟有可能psiClass是从缓存中获取的，但是被RestClassIconProvider修改了
      if (null == psiClass || !psiClass.isValid()) {
        continue;
      }
      httpMappingInfos.addAll(controllersOfPsiClass(psiClass, project));
    }
    // 将结果添加到缓存中
    BilateralCacheManager.initControllerCaches(project, httpMappingInfos);

    return httpMappingInfos;
  }

  /**
   * 创建出当前psiclass（controller）内的所有HttpMappingInfo
   */
  public static List<HttpMappingInfo> controllersOfPsiClass(PsiClass psiClass, Project project) {
    List<HttpMappingInfo> rs = new ArrayList<>();
    if (AnnotationParserUtils.isControllerClass(psiClass)) {
      StringBuilder parentPath = new StringBuilder();
      String serverPath = extractSpringProperties(psiClass, project, SPRINGBOOT_SERVER_PATH);
      String mvcPath = extractSpringProperties(psiClass, project, SPRINGMVC_PATH);
      String controllerPath = controllerPsiClassPath(psiClass);
      parentPath.append(serverPath).append(mvcPath).append(controllerPath);
      // 解析类中的方法，提取接口路径和Swagger注解信息
      PsiMethod[] methods = psiClass.getMethods();
      for (PsiMethod method : methods) {
        HttpMappingInfo httpMappingInfo = HttpMappingInfo.of(parentPath.toString(), method);
        if (httpMappingInfo != null) {
          // 设置psi方法信息
          httpMappingInfo.setPsiMethod(method);
          rs.add(httpMappingInfo);
        }
      }
    }
    return rs;
  }

  /**
   * 创建出当前psiclass（controller）内的,指定的psiMethod对应的HttpMappingInfo
   */
  public static HttpMappingInfo controllerOfPsiMethod(PsiClass psiClass, Project project,
      PsiMethod psiMethod) {
    HttpMappingInfo httpMappingInfo = null;
    if (AnnotationParserUtils.isControllerClass(psiClass)) {
      StringBuilder parentPath = new StringBuilder();
      String serverPath = extractSpringProperties(psiClass, project, SPRINGBOOT_SERVER_PATH);
      String mvcPath = extractSpringProperties(psiClass, project, SPRINGMVC_PATH);
      String controllerPath = controllerPsiClassPath(psiClass);
      parentPath.append(serverPath).append(mvcPath).append(controllerPath);
      // 提取接口路径和Swagger注解信息
      httpMappingInfo = HttpMappingInfo.of(parentPath.toString(), psiMethod);
      if (Objects.nonNull(httpMappingInfo)) {
        // 设置psi方法信息
        httpMappingInfo.setPsiMethod(psiMethod);
      }

    }
    return httpMappingInfo;
  }

  /**
   * resolve： eg.server.servlet.context-path=/hello eg .spring.mvc.servlet.path=/world
   *
   * @param psiClass
   * @param project
   * @param configKey
   * @return
   */
  public static String extractSpringProperties(PsiClass psiClass, Project project,
      String configKey) {
    Optional<PsiDirectory> serviceModuleDirectory = ServerParser.getServiceModuleResourcesDirectory(
        psiClass, project);
    String propertyPath = null;

    if (serviceModuleDirectory.isPresent()) {
      // 读取 properties 文件
      Properties properties = ConfigReader.readProperties(serviceModuleDirectory.get());
      if (properties != null && properties.containsKey(configKey)) {
        propertyPath = properties.getProperty(configKey);
      }

      // 如果在 properties 文件中未找到，继续在 yml 或 yaml 文件中查找
      if (propertyPath == null) {
        Map<String, Object> yml = ConfigReader.readYmlOrYaml(serviceModuleDirectory.get());
        if (yml != null) {
          propertyPath = extractValueFromYml(yml, configKey);
        }
      }
    }

    return propertyPath == null ? "" : propertyPath;
  }

  // 从 YAML Map 中提取目标值，支持嵌套键
  private static String extractValueFromYml(Map<String, Object> yml, String configKey) {
    String[] keys = configKey.split("\\.");
    Object value = yml;

    for (String key : keys) {
      if (value instanceof Map) {
        value = ((Map<?, ?>) value).get(key);
      } else {
        return null;
      }
    }

    return value != null ? value.toString() : null;
  }

  /**
   * 提取Controller类文件的接口路径
   *
   * @param psiClass psi类
   * @return {@link String}
   */
  public static String controllerPsiClassPath(PsiClass psiClass) {
    PsiAnnotation[] annotations = psiClass.getAnnotations();
    for (PsiAnnotation annotation : annotations) {
      String annotationName = annotation.getQualifiedName();
      if (REQUEST_MAPPING.getQualifiedName().equals(annotationName)) {
        return AnnotationParserUtils.getValueFromRestful(annotation);
      }
    }
    return "";
  }

  /**
   * 当前feign，扫描待跳转的所有目标controller
   *
   * @param psiMethod psi方法
   * @return {@link List}<{@link PsiElement}>
   */
  public static List<PsiElement> process(PsiMethod psiMethod) {
    List<PsiElement> elementList = new ArrayList<>();

    // 获取当前项目
    Project project = psiMethod.getProject();

    List<HttpMappingInfo> controllerInfos = scanControllerPaths(project);

    if (controllerInfos != null) {
      // 遍历 Controller 类的所有方法
      for (HttpMappingInfo controller : controllerInfos) {
        if (match2C(controller, psiMethod)) {
          elementList.add(controller.getPsiMethod());
        }
      }
    }
    return elementList;
  }

  /**
   * 用当前feign接口匹配目标Controller接口
   */
  public static boolean match2C(HttpMappingInfo controllerInfo, PsiMethod feignMethod) {
    HttpMappingInfo feignCache = BilateralCacheManager.getOrSetFeignCache(feignMethod);
    if (Objects.isNull(feignCache)) {
      return false;
    }
    String feignPath = feignCache.getPath();
    return StringUtils.equals(feignPath, controllerInfo.getPath());
  }
}