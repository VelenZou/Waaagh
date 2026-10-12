package com.waaagh.hierarchy;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.call.CalleeMethodsTreeStructure;
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor;
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
 * Call Hierarchy "Callees" tree, kept <b>recursive</b> and extended with the Feign &lt;-&gt; Controller edge:
 * <ul>
 *     <li>a Feign method lists the matching Controller methods as callees;</li>
 *     <li>every node — including a Controller node — is expanded with its own Java callees, so the tree
 *         continues into the server-side implementation.</li>
 * </ul>
 * <p>
 * Same rationale as {@link WaaaghCallerTreeStructure}: the platform's {@code CalleeMethodsTreeStructure}
 * is {@code final}, so the native callee computation is reused via composition instead of reimplementing it.
 */
public class WaaaghCalleeTreeStructure extends HierarchyTreeStructure {

    private final String myScopeType;

    public WaaaghCalleeTreeStructure(@NotNull Project project, @NotNull PsiMethod method, @NotNull String scopeType) {
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

        // Matching Controllers are the callees of a Feign client call.
        if (FeignControllerNavigator.isFeignMethod(method)) {
            for (PsiElement controller : FeignControllerNavigator.findControllers(method)) {
                addChild(children, descriptor, controller, seen);
            }
        }

        // Native Java callees, with a cycle guard (a re-rooted native structure cannot do it itself).
        for (PsiMethod callee : nativeCallees(method)) {
            if (isAncestor(descriptor, callee)) {
                continue;
            }
            addChild(children, descriptor, callee, seen);
        }

        return children.toArray(new HierarchyNodeDescriptor[0]);
    }

    private List<PsiMethod> nativeCallees(@NotNull PsiMethod method) {
        CalleeMethodsTreeStructure nativeStructure =
                new CalleeMethodsTreeStructure(myProject, method, myScopeType);
        List<PsiMethod> callees = new ArrayList<>();
        for (Object child : nativeStructure.getChildElements(nativeStructure.getBaseDescriptor())) {
            if (child instanceof CallHierarchyNodeDescriptor) {
                PsiMember enclosing = ((CallHierarchyNodeDescriptor) child).getEnclosingElement();
                if (enclosing instanceof PsiMethod && enclosing.isValid()) {
                    callees.add((PsiMethod) enclosing);
                }
            }
        }
        return callees;
    }

    private static boolean isAncestor(@NotNull HierarchyNodeDescriptor descriptor, @NotNull PsiMethod method) {
        HierarchyNodeDescriptor current = descriptor;
        while (current != null) {
            PsiMember enclosing = ((CallHierarchyNodeDescriptor) current).getEnclosingElement();
            if (enclosing == method) {
                return true;
            }
            current = current.getParentDescriptor() instanceof HierarchyNodeDescriptor
                    ? (HierarchyNodeDescriptor) current.getParentDescriptor()
                    : null;
        }
        return false;
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
