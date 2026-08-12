package com.waaagh.navigation;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.waaagh.cache.BilateralCacheManager;
import com.waaagh.utils.AnnotationParserUtils;
import com.waaagh.utils.ControllerClassScanUtils;
import com.waaagh.utils.FeignClassScanUtils;
import com.waaagh.utils.ProjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Shared Feign &lt;-&gt; Controller matching used by gutter icons, Go to Implementation,
 * Go to Declaration and Call Hierarchy contributors.
 */
public final class FeignControllerNavigator {

    private FeignControllerNavigator() {
    }

    @Nullable
    public static PsiMethod resolveMethod(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }
        if (element instanceof PsiMethod) {
            return (PsiMethod) element;
        }
        return PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);
    }

    public static boolean isFeignMethod(@Nullable PsiMethod method) {
        return method != null
                && method.isValid()
                && ProjectUtils.isBizElement(method)
                && AnnotationParserUtils.isElementWithinFeign(method)
                && AnnotationParserUtils.containsRestfulAnnotation(method);
    }

    public static boolean isControllerMethod(@Nullable PsiMethod method) {
        return method != null
                && method.isValid()
                && ProjectUtils.isBizElement(method)
                && AnnotationParserUtils.isElementWithinController(method)
                && AnnotationParserUtils.containsRestfulAnnotation(method);
    }

    @NotNull
    public static List<PsiElement> findControllers(@NotNull PsiMethod feignMethod) {
        Project project = feignMethod.getProject();
        if (DumbService.isDumb(project) || !isFeignMethod(feignMethod)) {
            return Collections.emptyList();
        }
        ControllerClassScanUtils.scanControllerPaths(project);
        BilateralCacheManager.setOrCoverFeignCache(feignMethod);
        return ControllerClassScanUtils.process(feignMethod);
    }

    @NotNull
    public static List<PsiElement> findFeignClients(@NotNull PsiMethod controllerMethod) {
        Project project = controllerMethod.getProject();
        if (DumbService.isDumb(project) || !isControllerMethod(controllerMethod)) {
            return Collections.emptyList();
        }
        FeignClassScanUtils.scanFeignInterfaces(project);
        BilateralCacheManager.setOrCoverControllerCache(controllerMethod);
        return FeignClassScanUtils.process(controllerMethod);
    }
}
