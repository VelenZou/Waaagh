package com.waaagh.navigation;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Powers IDE "Go to Implementation" (Ctrl+Alt+B) / Quick Definition for Feign methods.
 * Matching Controllers are treated as implementations of the Feign contract.
 */
public class FeignDefinitionsSearcher
        extends QueryExecutorBase<PsiElement, DefinitionsScopedSearch.SearchParameters> {

    public FeignDefinitionsSearcher() {
        super(true);
    }

    @Override
    public void processQuery(@NotNull DefinitionsScopedSearch.SearchParameters queryParameters,
                             @NotNull Processor<? super PsiElement> consumer) {
        PsiMethod method = FeignControllerNavigator.resolveMethod(queryParameters.getElement());
        if (!FeignControllerNavigator.isFeignMethod(method)) {
            return;
        }

        List<PsiElement> controllers = FeignControllerNavigator.findControllers(method);
        for (PsiElement controller : controllers) {
            if (controller != null && controller.isValid() && !consumer.process(controller)) {
                return;
            }
        }
    }
}
