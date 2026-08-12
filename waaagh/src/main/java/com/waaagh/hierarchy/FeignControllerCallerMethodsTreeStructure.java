package com.waaagh.hierarchy;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.ArrayUtil;
import com.waaagh.navigation.FeignControllerNavigator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Call Hierarchy "Callers" tree for a Controller method: each matching Feign client method
 * is treated as a caller of the Controller endpoint.
 * <p>
 * This mirrors {@link FeignCalleeMethodsTreeStructure} for the opposite direction and replaces the
 * previous {@code MethodReferencesSearch} approach — IntelliJ's {@code JavaCallReferenceProcessor}
 * only accepts {@code PsiReferenceExpression}/{@code LightMemberReference}, so synthetic references
 * never surfaced in the Callers view.
 */
public class FeignControllerCallerMethodsTreeStructure extends HierarchyTreeStructure {

    public FeignControllerCallerMethodsTreeStructure(@NotNull Project project, @NotNull PsiMethod method) {
        super(project, new CallHierarchyNodeDescriptor(project, null, method, true, false));
    }

    @Override
    protected Object @NotNull [] buildChildren(@NotNull HierarchyNodeDescriptor descriptor) {
        PsiElement element = ((CallHierarchyNodeDescriptor) descriptor).getTargetElement();
        if (!(element instanceof PsiMethod) || !element.isValid()) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        PsiMethod method = (PsiMethod) element;
        // Only the Controller root expands into Feign clients; Feign nodes stay leaves.
        if (!FeignControllerNavigator.isControllerMethod(method)) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        List<PsiElement> feignMethods = FeignControllerNavigator.findFeignClients(method);
        if (feignMethods.isEmpty()) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        Set<PsiMethod> seen = new HashSet<>();
        List<CallHierarchyNodeDescriptor> children = new ArrayList<>();
        for (PsiElement feign : feignMethods) {
            if (!(feign instanceof PsiMethod) || !feign.isValid()) {
                continue;
            }
            PsiMethod feignMethod = (PsiMethod) feign;
            if (!seen.add(feignMethod)) {
                continue;
            }
            children.add(new CallHierarchyNodeDescriptor(myProject, descriptor, feignMethod, false, false));
        }
        return children.toArray(new HierarchyNodeDescriptor[0]);
    }
}
