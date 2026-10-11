package com.waaagh.endpoints;

import com.intellij.microservices.endpoints.EndpointType;
import com.intellij.microservices.endpoints.EndpointTypes;
import com.intellij.microservices.endpoints.EndpointsFilter;
import com.intellij.microservices.endpoints.EndpointsUrlTargetProvider;
import com.intellij.microservices.endpoints.FrameworkPresentation;
import com.intellij.microservices.endpoints.ModuleEndpointsFilter;
import com.intellij.microservices.endpoints.presentation.HttpMethodPresentation;
import com.intellij.microservices.url.UrlTargetInfo;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.waaagh.cache.BilateralCacheManager;
import com.waaagh.constant.RestIcons;
import com.waaagh.entity.HttpMappingInfo;
import com.waaagh.utils.ControllerClassScanUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * WAAAGH 在 Endpoints 工具窗口中提供的服务端端点视图。
 * <p>
 * 与 Spring MVC 内置条目的差异：这里的路径完全由 WAAAGH 的解析链得出
 * （server.servlet.context-path + spring.mvc.servlet.path + PathMatchConfigurer 前缀 + 类/方法映射），
 * 因此 {@code WaaaghPathPrefixConfig} 这类动态前缀也包含在内。
 * <p>
 * 该类只在存在 Endpoints 插件（Ultimate）的 IDE 中通过 waaagh-endpoints.xml 注册并加载。
 */
public final class WaaaghEndpointsProvider
    implements EndpointsUrlTargetProvider<PsiClass, HttpMappingInfo> {

    private static final String CONTROLLER_ANNOTATION = "org.springframework.stereotype.Controller";

    private static final String REST_CONTROLLER_ANNOTATION =
        "org.springframework.web.bind.annotation.RestController";

    private static final FrameworkPresentation FRAMEWORK_PRESENTATION =
        new FrameworkPresentation("waaagh", "WAAAGH", RestIcons.WAAAGH_ENDPOINT_ICON);

    /** 按 Controller 类分组的端点缓存，随 PSI 修改计数失效。 */
    private static final Key<CachedValue<Map<PsiClass, List<HttpMappingInfo>>>> GROUPED_ENDPOINTS =
        Key.create("com.waaagh.endpoints.grouped");

    @Override
    public EndpointType getEndpointType() {
        return EndpointTypes.HTTP_SERVER_TYPE;
    }

    @Override
    public FrameworkPresentation getPresentation() {
        return FRAMEWORK_PRESENTATION;
    }

    @Override
    public Status getStatus(@NotNull Project project) {
        if (project.isDisposed() || DumbService.isDumb(project)) {
            return Status.UNAVAILABLE;
        }
        // getStatus 可能在 EDT 上被调用（框架筛选器），需要读锁
        boolean hasSpringMvc = ApplicationManager.getApplication().runReadAction(
            (Computable<Boolean>) () -> hasSpringMvcLibrary(project));
        return hasSpringMvc ? Status.HAS_ENDPOINTS : Status.UNAVAILABLE;
    }

    private static boolean hasSpringMvcLibrary(@NotNull Project project) {
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        return facade.findClass(CONTROLLER_ANNOTATION, scope) != null
            || facade.findClass(REST_CONTROLLER_ANNOTATION, scope) != null;
    }

    @Override
    public Iterable<PsiClass> getEndpointGroups(@NotNull Project project,
                                                @NotNull EndpointsFilter filter) {
        if (project.isDisposed() || DumbService.isDumb(project)) {
            return Collections.emptyList();
        }
        // 只服务于当前模块视图；外部端点（OpenAPI 等）由其它 provider 处理
        if (!(filter instanceof ModuleEndpointsFilter)) {
            return Collections.emptyList();
        }
        ModuleEndpointsFilter moduleFilter = (ModuleEndpointsFilter) filter;
        Module module = moduleFilter.getModule();
        boolean fromTests = moduleFilter.getFromTests();

        Map<PsiClass, List<HttpMappingInfo>> grouped = groupedEndpoints(project);
        if (grouped.isEmpty()) {
            return Collections.emptyList();
        }

        Set<PsiClass> groups = new LinkedHashSet<>();
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        for (PsiClass controllerClass : grouped.keySet()) {
            if (!controllerClass.isValid()) {
                continue;
            }
            if (module != null && !module.equals(ModuleUtilCore.findModuleForPsiElement(controllerClass))) {
                continue;
            }
            VirtualFile file = virtualFileOf(controllerClass);
            if (file == null) {
                continue;
            }
            if (!fromTests && fileIndex.isInTestSourceContent(file)) {
                continue;
            }
            groups.add(controllerClass);
        }
        return groups;
    }

    @Override
    public List<HttpMappingInfo> getEndpoints(@NotNull PsiClass group) {
        if (!group.isValid()) {
            return Collections.emptyList();
        }
        Map<PsiClass, List<HttpMappingInfo>> grouped = groupedEndpoints(group.getProject());
        List<HttpMappingInfo> endpoints = grouped.get(group);
        if (endpoints == null) {
            // PSI 实例可能已刷新，按限定名 + 文件回退匹配
            for (Map.Entry<PsiClass, List<HttpMappingInfo>> entry : grouped.entrySet()) {
                if (isSameClass(group, entry.getKey())) {
                    endpoints = entry.getValue();
                    break;
                }
            }
        }
        if (endpoints == null || endpoints.isEmpty()) {
            return Collections.emptyList();
        }
        List<HttpMappingInfo> result = new ArrayList<>(endpoints);
        result.sort(Comparator.comparingInt(mapping -> {
            PsiMethod method = mapping.getPsiMethod();
            return method == null ? Integer.MAX_VALUE : method.getTextOffset();
        }));
        return result;
    }

    @Override
    public boolean isValidEndpoint(@NotNull PsiClass group, @NotNull HttpMappingInfo endpoint) {
        return group.isValid() && WaaaghEndpointsUtil.isUsable(endpoint.getPsiMethod());
    }

    @Override
    public ItemPresentation getEndpointPresentation(@NotNull PsiClass group,
                                                    @NotNull HttpMappingInfo endpoint) {
        String url = WaaaghEndpointsUtil.normalizeUrl(endpoint.getPath());
        Set<String> requestMethods = endpoint.getRequestMethods();
        List<String> methods = requestMethods == null || requestMethods.isEmpty()
            ? Collections.emptyList()
            : new ArrayList<>(requestMethods);
        String location = group.getName();
        boolean deprecated = WaaaghEndpointsUtil.isDeprecated(endpoint.getPsiMethod());
        return new HttpMethodPresentation(
            url,
            methods,
            location,
            RestIcons.WAAAGH_ENDPOINT_ICON,
            deprecated ? CodeInsightColors.DEPRECATED_ATTRIBUTES : null);
    }

    @Override
    public ModificationTracker getModificationTracker(@NotNull Project project) {
        return PsiModificationTracker.getInstance(project);
    }

    @Override
    public PsiElement getDocumentationElement(@NotNull PsiClass group,
                                              @NotNull HttpMappingInfo endpoint) {
        return endpoint.getPsiMethod();
    }

    @Override
    public PsiElement getNavigationElement(@NotNull PsiClass group,
                                           @NotNull HttpMappingInfo endpoint) {
        return endpoint.getPsiMethod();
    }

    @Override
    public Iterable<UrlTargetInfo> getUrlTargetInfo(@NotNull PsiClass group,
                                                    @NotNull HttpMappingInfo endpoint) {
        return Collections.singletonList(WaaaghEndpointsUtil.createUrlTargetInfo(endpoint));
    }

    // ------------------------------------------------------------------
    // 缓存
    // ------------------------------------------------------------------

    /**
     * 全量扫描（如未完成）并按 Controller 类分组。
     * 分组结果缓存在 Project 上，依赖 PSI 修改计数；底层数据来自 WAAAGH 的双向缓存，
     * 该缓存在文件保存 / 外部修改同步后由监听器清理，下次查询自动重扫。
     */
    @NotNull
    private static Map<PsiClass, List<HttpMappingInfo>> groupedEndpoints(@NotNull Project project) {
        if (project.isDisposed() || DumbService.isDumb(project)) {
            return Collections.emptyMap();
        }
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            GROUPED_ENDPOINTS,
            () -> CachedValueProvider.Result.create(
                buildGroupedEndpoints(project), PsiModificationTracker.getInstance(project)),
            false);
    }

    @NotNull
    private static Map<PsiClass, List<HttpMappingInfo>> buildGroupedEndpoints(@NotNull Project project) {
        if (!BilateralCacheManager.isControllerFullyScanned(project)) {
            ControllerClassScanUtils.scanControllerPaths(project);
        }
        Map<String, HttpMappingInfo> caches = BilateralCacheManager.queryControllerCaches(project);
        if (caches == null || caches.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<PsiClass, List<HttpMappingInfo>> grouped = new LinkedHashMap<>();
        for (HttpMappingInfo mapping : caches.values()) {
            PsiClass controllerClass = controllerClassOf(mapping);
            if (controllerClass == null) {
                continue;
            }
            grouped.computeIfAbsent(controllerClass, key -> new ArrayList<>()).add(mapping);
        }
        return grouped;
    }

    private static PsiClass controllerClassOf(@NotNull HttpMappingInfo mapping) {
        PsiMethod method = mapping.getPsiMethod();
        if (!WaaaghEndpointsUtil.isUsable(method)) {
            return null;
        }
        PsiClass controllerClass = method.getContainingClass();
        return controllerClass != null && controllerClass.isValid() ? controllerClass : null;
    }

    private static boolean isSameClass(@NotNull PsiClass left, @NotNull PsiClass right) {
        if (left.equals(right) || left == right) {
            return true;
        }
        String leftName = left.getQualifiedName();
        String rightName = right.getQualifiedName();
        return leftName != null && leftName.equals(rightName)
            && Objects.equals(virtualFileOf(left), virtualFileOf(right));
    }

    private static VirtualFile virtualFileOf(@NotNull PsiElement element) {
        PsiFile file = WaaaghEndpointsUtil.containingFile(element);
        return file == null ? null : file.getVirtualFile();
    }
}
