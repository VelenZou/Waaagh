package com.waaagh.hierarchy;

import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.call.CallHierarchyBrowser;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.waaagh.navigation.FeignControllerNavigator;
import org.jetbrains.annotations.NotNull;

/**
 * Call Hierarchy browser for the Feign &lt;-&gt; Controller mapping:
 * <ul>
 *     <li>Feign method → <b>Callees</b> are the matching Controllers;</li>
 *     <li>Controller method → <b>Callers</b> are the matching Feign clients.</li>
 * </ul>
 * The opposite view of each (and everything non-mapping) falls back to the standard Java tree.
 */
public class WaaaghCallHierarchyBrowser extends CallHierarchyBrowser {

    private final PsiMethod target;

    public WaaaghCallHierarchyBrowser(@NotNull Project project, @NotNull PsiMethod method) {
        super(project, method);
        this.target = method;
    }

    /**
     * Opens the most relevant view for the target: Callees for a Feign method (its body is empty,
     * so Controllers are the interesting direction), Callers for a Controller method (Feign clients).
     */
    public void activateInitialView() {
        if (FeignControllerNavigator.isFeignMethod(target)) {
            changeView(CALLEE_TYPE);
        } else {
            changeView(CALLER_TYPE);
        }
    }

    @Override
    protected HierarchyTreeStructure createHierarchyTreeStructure(@NotNull String typeName,
                                                                  @NotNull PsiElement psiElement) {
        if (isCalleeType(typeName)
                && psiElement instanceof PsiMethod
                && FeignControllerNavigator.isFeignMethod((PsiMethod) psiElement)) {
            return new FeignCalleeMethodsTreeStructure(myProject, (PsiMethod) psiElement);
        }
        if (isCallerType(typeName)
                && psiElement instanceof PsiMethod
                && FeignControllerNavigator.isControllerMethod((PsiMethod) psiElement)) {
            return new FeignControllerCallerMethodsTreeStructure(myProject, (PsiMethod) psiElement);
        }
        return super.createHierarchyTreeStructure(typeName, psiElement);
    }

    // The framework keys tree structures by the localized view title; match both the raw constant
    // and the localized label to stay correct across locales.
    private boolean isCalleeType(String typeName) {
        return CALLEE_TYPE.equals(typeName) || getCalleeType().equals(typeName);
    }

    private boolean isCallerType(String typeName) {
        return CALLER_TYPE.equals(typeName) || getCallerType().equals(typeName);
    }
}
