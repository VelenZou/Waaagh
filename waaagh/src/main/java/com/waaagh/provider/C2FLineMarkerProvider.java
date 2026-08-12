package com.waaagh.provider;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.waaagh.constant.RestIcons;
import com.waaagh.navigation.FeignControllerNavigator;
import com.waaagh.utils.AnnotationParserUtils;
import com.waaagh.utils.ProjectUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Gutter navigation from Controller methods to matching Feign clients.
 */
public class C2FLineMarkerProvider extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element,
                                            @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!ProjectUtils.isBizElement(element) || DumbService.isDumb(element.getProject())) {
            return;
        }
        if (!(element instanceof PsiMethod)) {
            return;
        }
        PsiMethod method = (PsiMethod) element;
        if (!FeignControllerNavigator.isControllerMethod(method)) {
            return;
        }

        PsiAnnotation restfulAnnotation = AnnotationParserUtils.findRestfulAnnotation(method);
        if (restfulAnnotation == null) {
            return;
        }

        List<PsiElement> resultList = FeignControllerNavigator.findFeignClients(method);
        if (!resultList.isEmpty()) {
            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder
                    .create(RestIcons.STATEMENT_LINE_CONTROLLER_ICON)
                    .setAlignment(GutterIconRenderer.Alignment.RIGHT)
                    .setTargets(resultList)
                    .setTooltipTitle("Navigation to target in Feign");
            result.add(builder.createLineMarkerInfo(restfulAnnotation));
        }
    }
}
