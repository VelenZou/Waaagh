package com.waaagh.navigation;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Powers Ctrl+B / Ctrl+Click navigation between Feign methods and matching Controllers.
 */
public class FeignGotoDeclarationHandler implements GotoDeclarationHandler {

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement,
                                                             int offset,
                                                             Editor editor) {
        PsiMethod method = FeignControllerNavigator.resolveMethod(sourceElement);
        if (method == null) {
            return null;
        }

        List<PsiElement> targets;
        if (FeignControllerNavigator.isFeignMethod(method)) {
            targets = FeignControllerNavigator.findControllers(method);
        } else if (FeignControllerNavigator.isControllerMethod(method)) {
            targets = FeignControllerNavigator.findFeignClients(method);
        } else {
            return null;
        }

        if (targets.isEmpty()) {
            return null;
        }
        return targets.toArray(new PsiElement[0]);
    }
}
