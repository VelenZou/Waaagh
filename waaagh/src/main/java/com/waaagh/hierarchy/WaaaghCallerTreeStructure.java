package com.waaagh.hierarchy;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.call.CallerMethodsTreeStructure;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.util.ArrayUtilRt;
import com.waaagh.navigation.FeignControllerNavigator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Call Hierarchy "Callers" tree, kept <b>recursive</b> and extended with the Feign &lt;-&gt; Controller edge:
 * <ul>
 *     <li>a Controller endpoint also lists the matching Feign client methods as callers (remote callers);</li>
 *     <li>every node — including a Feign node — is expanded with its own Java callers, so the tree can keep
 *         walking upwards past the Feign client (the business code that calls it).</li>
 * </ul>
 * <p>
 * The platform's {@code CallerMethodsTreeStructure} is {@code final} (and the additive
 * {@code com.intellij.hierarchy.elementProvider} EP is far newer than this plugin's {@code since-build}),
 * so the native caller computation is reused by running a throw-away native tree rooted at the node's
 * method and taking its first level of children — no native behavior is reimplemented by hand.
 */
public class WaaaghCallerTreeStructure extends HierarchyTreeStructure {

    private final String myScopeType;

    public WaaaghCallerTreeStructure(@NotNull Project project, @NotNull PsiMethod method, @NotNull String scopeType) {
        super(project, new CallHierarchyNodeDescriptor(project, null, method, true, false));
        myScopeType = scopeType;
    }

    @Override
    protected Object @NotNull [] buildChildren(@NotNull HierarchyNodeDescriptor descriptor) {
        PsiElement element = ((CallHierarchyNodeDescriptor) descriptor).getEnclosingElement();
        if (!(element instanceof PsiMethod) || !element.isValid()) {
            return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
        }

        PsiMethod method = (PsiMethod) element;
        Set<PsiMethod> seen = new HashSet<>();
        List<CallHierarchyNodeDescriptor> children = new ArrayList<>();

        // Feign clients are the remote callers of a Controller endpoint.
        if (FeignControllerNavigator.isControllerMethod(method)) {
            for (PsiElement feign : FeignControllerNavigator.findFeignClients(method)) {
                addChild(children, descriptor, feign, seen);
            }
        }

        // Native Java callers — this is what keeps the tree going above a Feign node.
        for (PsiMethod caller : nativeCallers(method)) {
            addChild(children, descriptor, caller, seen);
        }

        return children.toArray(new HierarchyNodeDescriptor[0]);
    }

    private List<PsiMethod> nativeCallers(@NotNull PsiMethod method) {
        CallerMethodsTreeStructure nativeStructure =
                new CallerMethodsTreeStructure(myProject, method, myScopeType);
        List<PsiMethod> callers = new ArrayList<>();
        for (Object child : nativeStructure.getChildElements(nativeStructure.getBaseDescriptor())) {
            if (child instanceof CallHierarchyNodeDescriptor) {
                PsiMember enclosing = ((CallHierarchyNodeDescriptor) child).getEnclosingElement();
                if (enclosing instanceof PsiMethod && enclosing.isValid()) {
                    callers.add((PsiMethod) enclosing);
                }
            }
        }
        return callers;
    }

    private void addChild(@NotNull List<CallHierarchyNodeDescriptor> children,
                          @NotNull HierarchyNodeDescriptor parent,
                          @NotNull PsiElement element,
                          @NotNull Set<PsiMethod> seen) {
        if (!(element instanceof PsiMethod) || !element.isValid()) {
            return;
        }
        PsiMethod method = (PsiMethod) element;
        if (!seen.add(method)) {
            return;
        }
        children.add(new CallHierarchyNodeDescriptor(myProject, parent, method, false, false));
    }
}
