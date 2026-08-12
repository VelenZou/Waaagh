package com.waaagh.hierarchy;

import com.intellij.ide.hierarchy.CallHierarchyBrowserBase;
import com.intellij.ide.hierarchy.HierarchyBrowser;
import com.intellij.ide.hierarchy.HierarchyProvider;
import com.intellij.ide.hierarchy.call.JavaCallHierarchyProvider;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.waaagh.navigation.FeignControllerNavigator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Call Hierarchy provider that keeps Java's default behavior for ordinary methods,
 * but shows Feign -&gt; Controller mappings as callees for Feign client methods.
 * <p>
 * Registered with {@code order="first"} and delegates everything non-Feign to
 * {@link JavaCallHierarchyProvider}, so normal Java Call Hierarchy is preserved.
 * Controller -&gt; Feign appears via {@code MethodReferencesSearch} (Callers view).
 */
public class WaaaghCallHierarchyProvider implements HierarchyProvider {

    private final JavaCallHierarchyProvider javaDelegate = new JavaCallHierarchyProvider();

    @Override
    public @Nullable PsiElement getTarget(@NotNull DataContext dataContext) {
        return javaDelegate.getTarget(dataContext);
    }

    @Override
    public @NotNull HierarchyBrowser createHierarchyBrowser(@NotNull PsiElement target) {
        if (target instanceof PsiMethod && FeignControllerNavigator.isFeignMethod((PsiMethod) target)) {
            return new WaaaghCallHierarchyBrowser(target.getProject(), (PsiMethod) target);
        }
        return javaDelegate.createHierarchyBrowser(target);
    }

    @Override
    public void browserActivated(@NotNull HierarchyBrowser hierarchyBrowser) {
        if (hierarchyBrowser instanceof WaaaghCallHierarchyBrowser) {
            // Feign methods have no Java body; open Callees first to show Controllers.
            ((WaaaghCallHierarchyBrowser) hierarchyBrowser).changeView(CallHierarchyBrowserBase.CALLEE_TYPE);
        } else {
            javaDelegate.browserActivated(hierarchyBrowser);
        }
    }
}
