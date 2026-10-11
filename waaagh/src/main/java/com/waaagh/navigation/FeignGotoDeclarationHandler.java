package com.waaagh.navigation;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Powers Ctrl+B / Ctrl+Click navigation between Feign methods and matching Controllers.
 * <p>
 * Only the method <em>declaration</em> itself (the method name or the {@link PsiMethod}) is handled.
 * For anything inside a method body — fields, locals, calls, parameters, annotations — the handler
 * stays silent so IDEA's native Go to Declaration keeps working (e.g. jumping from
 * {@code userService.getUserById()} to the called method instead of the enclosing Controller method).
 */
public class FeignGotoDeclarationHandler implements GotoDeclarationHandler {

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement,
                                                             int offset,
                                                             Editor editor) {
        PsiMethod method = resolveDeclaredMethod(sourceElement);
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

    /**
     * Resolves the method only when the caret sits on its declaration (its name identifier),
     * never when it sits on something inside the method body.
     */
    @Nullable
    private static PsiMethod resolveDeclaredMethod(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }
        if (element instanceof PsiMethod) {
            return (PsiMethod) element;
        }
        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);
        if (method == null) {
            return null;
        }
        PsiElement nameIdentifier = method.getNameIdentifier();
        return nameIdentifier != null && PsiTreeUtil.isAncestor(nameIdentifier, element, false)
                ? method
                : null;
    }
}
