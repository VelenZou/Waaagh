package com.waaagh.hierarchy;

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
 * but repurposes the mapping views for Feign &lt;-&gt; Controller:
 * <ul>
 *     <li>Feign method → <b>Callees</b> are the matching Controllers;</li>
 *     <li>Controller method → <b>Callers</b> are the matching Feign clients.</li>
 * </ul>
 * Registered with {@code order="first"} and delegates everything non-mapping to
 * {@link JavaCallHierarchyProvider}, so normal Java Call Hierarchy is preserved.
 */
public class WaaaghCallHierarchyProvider implements HierarchyProvider {

    private final JavaCallHierarchyProvider javaDelegate = new JavaCallHierarchyProvider();

    @Override
    public @Nullable PsiElement getTarget(@NotNull DataContext dataContext) {
        return javaDelegate.getTarget(dataContext);
    }

    @Override
    public @NotNull HierarchyBrowser createHierarchyBrowser(@NotNull PsiElement target) {
        if (target instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) target;
            if (FeignControllerNavigator.isFeignMethod(method) || FeignControllerNavigator.isControllerMethod(method)) {
                return new WaaaghCallHierarchyBrowser(target.getProject(), method);
            }
        }
        return javaDelegate.createHierarchyBrowser(target);
    }

    @Override
    public void browserActivated(@NotNull HierarchyBrowser hierarchyBrowser) {
        if (hierarchyBrowser instanceof WaaaghCallHierarchyBrowser) {
            // Feign -> open Callees (Controllers); Controller -> open Callers (Feign clients).
            ((WaaaghCallHierarchyBrowser) hierarchyBrowser).activateInitialView();
        } else {
            javaDelegate.browserActivated(hierarchyBrowser);
        }
    }
}
