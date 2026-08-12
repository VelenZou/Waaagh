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
 * Call Hierarchy "Callees" tree for a Feign method: each matching Controller method
 * is treated as a callee of the Feign client call.
 */
public class FeignCalleeMethodsTreeStructure extends HierarchyTreeStructure {

    public FeignCalleeMethodsTreeStructure(@NotNull Project project, @NotNull PsiMethod method) {
        super(project, new CallHierarchyNodeDescriptor(project, null, method, true, false));
    }

    @Override
    protected Object @NotNull [] buildChildren(@NotNull HierarchyNodeDescriptor descriptor) {
        HierarchyNodeDescriptor nodeDescriptor = descriptor;
        PsiElement element = ((CallHierarchyNodeDescriptor) nodeDescriptor).getTargetElement();
        if (!(element instanceof PsiMethod) || !element.isValid()) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        PsiMethod method = (PsiMethod) element;
        // Only the Feign root expands into Controllers; Controller nodes then show their Java callees via refresh.
        if (!FeignControllerNavigator.isFeignMethod(method)) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        List<PsiElement> controllers = FeignControllerNavigator.findControllers(method);
        if (controllers.isEmpty()) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        Set<PsiMethod> seen = new HashSet<>();
        List<CallHierarchyNodeDescriptor> children = new ArrayList<>();
        for (PsiElement controller : controllers) {
            if (!(controller instanceof PsiMethod) || !controller.isValid()) {
                continue;
            }
            PsiMethod controllerMethod = (PsiMethod) controller;
            if (!seen.add(controllerMethod)) {
                continue;
            }
            children.add(new CallHierarchyNodeDescriptor(myProject, nodeDescriptor, controllerMethod, false, false));
        }
        return children.toArray(new HierarchyNodeDescriptor[0]);
    }
}
