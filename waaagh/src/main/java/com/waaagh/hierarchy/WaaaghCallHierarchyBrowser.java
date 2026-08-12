package com.waaagh.hierarchy;

import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.call.CallHierarchyBrowser;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.waaagh.navigation.FeignControllerNavigator;
import org.jetbrains.annotations.NotNull;

/**
 * Call Hierarchy browser for Feign methods: Callees are matching Controllers;
 * Callers fall back to the standard Java tree structure.
 */
public class WaaaghCallHierarchyBrowser extends CallHierarchyBrowser {

    public WaaaghCallHierarchyBrowser(@NotNull Project project, @NotNull PsiMethod method) {
        super(project, method);
    }

    @Override
    protected HierarchyTreeStructure createHierarchyTreeStructure(@NotNull String typeName,
                                                                  @NotNull PsiElement psiElement) {
        if (CALLEE_TYPE.equals(typeName)
                && psiElement instanceof PsiMethod
                && FeignControllerNavigator.isFeignMethod((PsiMethod) psiElement)) {
            return new FeignCalleeMethodsTreeStructure(myProject, (PsiMethod) psiElement);
        }
        return super.createHierarchyTreeStructure(typeName, psiElement);
    }
}
