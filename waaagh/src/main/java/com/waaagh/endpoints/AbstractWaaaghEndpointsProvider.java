package com.waaagh.endpoints;

import com.intellij.microservices.endpoints.EndpointType;
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
import com.waaagh.cache.BilateralCacheManager;
import com.waaagh.entity.HttpMappingInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

/**
 * WAAAGH Endpoints 提供者的公共实现：组 = 端点方法所属类（Controller / Feign 接口），
 * 端点 = WAAAGH 解析出的 {@link HttpMappingInfo}（完整路径、HTTP 方法、文档元素）。
 * <p>
 * 子类只区分框架元信息（图标 / 标题 / Type）与扫描数据源（MVC Controller 或 Feign Client）。
 * 这些类只在存在 Endpoints 插件（Ultimate）的 IDE 中通过 waaagh-endpoints.xml 注册并加载。
 */
abstract class AbstractWaaaghEndpointsProvider
    implements EndpointsUrlTargetProvider<PsiClass, HttpMappingInfo> {

    /** 每个子类一个分组缓存 Key（按端点方法所属类分组）。 */
    private static final Map<String, Key<CachedValue<Map<PsiClass, List<HttpMappingInfo>>>>> GROUPED_KEYS =
        new ConcurrentHashMap<>();

    private volatile FrameworkPresentation presentation;

    // ------------------------------------------------------------------
    // 子类差异
    // ------------------------------------------------------------------

    /** Framework 筛选器的查询标记，例如 {@code framework:waaagh-mvc}。 */
    protected abstract String frameworkId();

    /** Framework 筛选器与条目上展示的框架名。 */
    protected abstract String frameworkTitle();

    protected abstract Icon icon();

    protected abstract EndpointType type();

    /** classpath 上存在其中任意一个类时，该框架出现在 Endpoints 中。 */
    protected abstract String[] frameworkClassNames();

    protected abstract boolean fullyScanned(@NotNull Project project);

    protected abstract void scan(@NotNull Project project);

    protected abstract Map<String, HttpMappingInfo> caches(@NotNull Project project);

    // ------------------------------------------------------------------
    // EndpointsProvider
    // ------------------------------------------------------------------

    @Override
    public EndpointType getEndpointType() {
        return type();
    }

    @Override
    public FrameworkPresentation getPresentation() {
        FrameworkPresentation current = presentation;
        if (current == null) {
            current = new FrameworkPresentation(frameworkId(), frameworkTitle(), icon());
            presentation = current;
        }
        return current;
    }

    @Override
    public Status getStatus(@NotNull Project project) {
        if (project.isDisposed() || DumbService.isDumb(project)) {
            return Status.UNAVAILABLE;
        }
        // getStatus 可能在 EDT 上被调用（框架筛选器），需要读锁
        boolean available = ApplicationManager.getApplication().runReadAction(
            (Computable<Boolean>) () -> hasFrameworkClass(project));
        return available ? Status.HAS_ENDPOINTS : Status.UNAVAILABLE;
    }

    private boolean hasFrameworkClass(@NotNull Project project) {
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        for (String className : frameworkClassNames()) {
            if (facade.findClass(className, scope) != null) {
                return true;
            }
        }
        return false;
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
        for (PsiClass endpointClass : grouped.keySet()) {
            if (!endpointClass.isValid()) {
                continue;
            }
            if (module != null && !module.equals(ModuleUtilCore.findModuleForPsiElement(endpointClass))) {
                continue;
            }
            VirtualFile file = virtualFileOf(endpointClass);
            if (file == null) {
                continue;
            }
            if (!fromTests && fileIndex.isInTestSourceContent(file)) {
                continue;
            }
            groups.add(endpointClass);
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
            icon(),
            deprecated ? CodeInsightColors.DEPRECATED_ATTRIBUTES : null);
    }

    @Override
    public ModificationTracker getModificationTracker(@NotNull Project project) {
        // WAAAGH 自己的数据版本：缓存被清理（保存、PSI 编辑停顿、索引完成、模块结构变化）或
        // 全量扫描完成时变化，而不是全局 PSI 计数——后者会让 Endpoints 视图在 HTTP Client
        // 面板自身产生的 PSI 变化上反复重载（面板抖动）。
        // 编辑器内的 PSI 编辑由 ProjectCacheInvalidator 防抖后清理缓存并 +1：视图在编辑停顿后
        // 刷新一次，而不是每敲一个字符刷新一次。
        return BilateralCacheManager.getModificationTracker(project);
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
        return Collections.singletonList(WaaaghEndpointsUtil.createUrlTargetInfo(endpoint, icon()));
    }

    // ------------------------------------------------------------------
    // 缓存
    // ------------------------------------------------------------------

    /**
     * 全量扫描（如未完成）并按端点方法所属类分组。
     * 分组结果缓存在 Project 上，依赖 WAAAGH 数据版本；
     * 底层缓存在文件保存 / 外部修改同步后由监听器清理，下次查询自动重扫。
     */
    @NotNull
    private Map<PsiClass, List<HttpMappingInfo>> groupedEndpoints(@NotNull Project project) {
        if (project.isDisposed() || DumbService.isDumb(project)) {
            return Collections.emptyMap();
        }
        Key<CachedValue<Map<PsiClass, List<HttpMappingInfo>>>> key = GROUPED_KEYS.computeIfAbsent(
            getClass().getName(), name -> Key.create(name));
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            key,
            () -> CachedValueProvider.Result.create(
                buildGroupedEndpoints(project), BilateralCacheManager.getModificationTracker(project)),
            false);
    }

    @NotNull
    private Map<PsiClass, List<HttpMappingInfo>> buildGroupedEndpoints(@NotNull Project project) {
        if (!fullyScanned(project)) {
            scan(project);
        }
        Map<String, HttpMappingInfo> caches = caches(project);
        if (caches == null || caches.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<PsiClass, List<HttpMappingInfo>> grouped = new LinkedHashMap<>();
        for (HttpMappingInfo mapping : caches.values()) {
            PsiClass endpointClass = endpointClassOf(mapping);
            if (endpointClass == null) {
                continue;
            }
            grouped.computeIfAbsent(endpointClass, key -> new ArrayList<>()).add(mapping);
        }
        return grouped;
    }

    private static PsiClass endpointClassOf(@NotNull HttpMappingInfo mapping) {
        PsiMethod method = mapping.getPsiMethod();
        if (!WaaaghEndpointsUtil.isUsable(method)) {
            return null;
        }
        PsiClass containingClass = method.getContainingClass();
        return containingClass != null && containingClass.isValid() ? containingClass : null;
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
