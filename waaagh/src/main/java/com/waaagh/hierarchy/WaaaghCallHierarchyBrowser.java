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
 * Both trees stay recursive: each node is additionally expanded with its own Java callers / callees,
 * so the hierarchy keeps walking past the Feign/Controller edge in both directions.
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
            changeView(getCalleeType());
        } else {
            changeView(getCallerType());
        }
    }

    @Override
    protected HierarchyTreeStructure createHierarchyTreeStructure(@NotNull String typeName,
                                                                  @NotNull PsiElement psiElement) {
        if (psiElement instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) psiElement;
            if (isCalleeType(typeName)) {
                return new WaaaghCalleeTreeStructure(myProject, method, getCurrentScopeType());
            }
            if (isCallerType(typeName)) {
                return new WaaaghCallerTreeStructure(myProject, method, getCurrentScopeType());
            }
        }
        return super.createHierarchyTreeStructure(typeName, psiElement);
    }

    // The framework keys tree structures by the type name returned by these accessors
    // (localized view titles), so match against them directly.
    private boolean isCalleeType(String typeName) {
        return getCalleeType().equals(typeName);
    }

    private boolean isCallerType(String typeName) {
        return getCallerType().equals(typeName);
    }
}
