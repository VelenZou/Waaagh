package com.waaagh.utils;

import static com.waaagh.enums.SpringBootMethodAnnotation.REQUEST_MAPPING;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.CachedValuesManager;
import com.waaagh.cache.BilateralCacheManager;
import com.waaagh.entity.HttpMappingInfo;
import com.waaagh.enums.SpringBootClassAnnotation;
import com.waaagh.properties.ConfigReader;
import com.waaagh.properties.ServerParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

public class ControllerClassScanUtils {

  private static final String SPRINGBOOT_SERVER_PATH = "server.servlet.context-path";
  private static final String SPRINGMVC_PATH = "spring.mvc.servlet.path";

  private ControllerClassScanUtils() {
  }

  /**
   * 全量扫描工程中的 controllerinfos
   */
  public static List<HttpMappingInfo> scanControllerPaths(Project project) {
    // 检查是否在 Dumb 模式下，以避免在项目构建期间执行代码
    if (DumbService.isDumb(project)) {
      return Collections.emptyList();
    }

    // 只有全量扫描完成后才能直接使用缓存；按需补全的单方法缓存不能当作全量结果
    if (BilateralCacheManager.isControllerFullyScanned(project)) {
      Map<String, HttpMappingInfo> controllerCaches = BilateralCacheManager.queryControllerCaches(
          project);
      return controllerCaches == null ? Collections.emptyList()
          : new ArrayList<>(controllerCaches.values());
    }

    // 通过注解索引查找 Controller 类，避免全量递归遍历项目中的所有包
    List<HttpMappingInfo> httpMappingInfos = new ArrayList<>();
    for (PsiClass psiClass : findControllerClasses(project)) {
      // 校验 psiClass 的有效性，毕竟有可能psiClass是从索引中获取的，但已经被修改了
      if (null == psiClass || !psiClass.isValid()) {
        continue;
      }
      httpMappingInfos.addAll(controllersOfPsiClass(psiClass, project));
    }
    // 将结果添加到缓存中
    BilateralCacheManager.initControllerCaches(project, httpMappingInfos);
    BilateralCacheManager.markControllerFullyScanned(project);

    return httpMappingInfos;
  }

  /**
   * 基于注解索引查找项目中的 Controller 类（@Controller / @RestController）
   */
  private static List<PsiClass> findControllerClasses(Project project) {
    // 注意：@Controller/@RestController 来自依赖库，projectScope 不包含库中的 class，
    // 必须用 allScope 才能解析到注解类；结果再用 isBizElement 过滤为项目源码。
    GlobalSearchScope scope = GlobalSearchScope.allScope(project);
    JavaPsiFacade facade = JavaPsiFacade.getInstance(project);

    Set<PsiClass> controllerClasses = new LinkedHashSet<>();
    for (SpringBootClassAnnotation annotation : new SpringBootClassAnnotation[]{
        SpringBootClassAnnotation.CONTROLLER, SpringBootClassAnnotation.RESTCONTROLLER}) {
      PsiClass annotationClass = facade.findClass(annotation.getQualifiedName(), scope);
      if (annotationClass == null) {
        continue;
      }
      for (PsiClass psiClass : AnnotatedElementsSearch.searchPsiClasses(annotationClass, scope)
          .findAll()) {
        // 排除三方依赖，只保留项目源码中的类
        if (psiClass.isValid() && ProjectUtils.isBizElement(psiClass)) {
          controllerClasses.add(psiClass);
        }
      }
    }
    return new ArrayList<>(controllerClasses);
  }

  /**
   * 创建出当前psiclass（controller）内的所有HttpMappingInfo
   */
  public static List<HttpMappingInfo> controllersOfPsiClass(PsiClass psiClass, Project project) {
    List<HttpMappingInfo> rs = new ArrayList<>();
    if (AnnotationParserUtils.isControllerClass(psiClass)) {
      String parentPath = buildControllerParentPath(psiClass, project);
      // 解析类中的方法，提取接口路径和Swagger注解信息
      PsiMethod[] methods = psiClass.getMethods();
      for (PsiMethod method : methods) {
        HttpMappingInfo httpMappingInfo = HttpMappingInfo.of(parentPath, method);
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
      httpMappingInfo = HttpMappingInfo.of(buildControllerParentPath(psiClass, project), psiMethod);
      if (Objects.nonNull(httpMappingInfo)) {
        // 设置psi方法信息
        httpMappingInfo.setPsiMethod(psiMethod);
      }
    }
    return httpMappingInfo;
  }

  /**
   * 拼接类级别的前缀路径：server.servlet.context-path + spring.mvc.servlet.path + 类上的 @RequestMapping 路径
   */
  private static String buildControllerParentPath(PsiClass psiClass, Project project) {
    StringBuilder parentPath = new StringBuilder();
    Optional<PsiDirectory> resourcesDirectory = ServerParser.getServiceModuleResourcesDirectory(
        psiClass, project);
    if (resourcesDirectory.isPresent()) {
      parentPath.append(extractSpringProperties(resourcesDirectory.get(), SPRINGBOOT_SERVER_PATH));
      parentPath.append(extractSpringProperties(resourcesDirectory.get(), SPRINGMVC_PATH));
    }
    parentPath.append(controllerPsiClassPath(psiClass));
    return parentPath.toString();
  }

  /**
   * 按模块（resources 目录）缓存配置解析结果，避免每个 Controller 类都重新读取/解析一次配置文件。
   * 缓存依赖 PSI 修改计数，配置文件变更后会自动失效重读。
   */
  private static String extractSpringProperties(PsiDirectory resourcesDirectory, String configKey) {
    ModuleConfig moduleConfig = getModuleConfig(resourcesDirectory);
    if (moduleConfig.properties.containsKey(configKey)) {
      return moduleConfig.properties.getProperty(configKey);
    }
    String propertyPath = extractValueFromYml(moduleConfig.yml, configKey);
    return propertyPath == null ? "" : propertyPath;
  }

  private static ModuleConfig getModuleConfig(PsiDirectory resourcesDirectory) {
    return CachedValuesManager.getProjectPsiDependentCache(resourcesDirectory,
        directory -> new ModuleConfig(ConfigReader.readProperties(directory),
            ConfigReader.readYmlOrYaml(directory)));
  }

  private static final class ModuleConfig {

    private final Properties properties;
    private final Map<String, Object> yml;

    private ModuleConfig(Properties properties, Map<String, Object> yml) {
      this.properties = properties;
      this.yml = yml;
    }
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
