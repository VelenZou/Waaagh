package com.waaagh.navigation;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Contributes Feign client methods as synthetic references to matching Controller methods.
 * <p>
 * Java Call Hierarchy (Callers) uses {@link MethodReferencesSearch}, so Feign clients appear
 * as callers of the Controller endpoint they map to.
 */
public class FeignMethodReferencesSearcher
        extends QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters> {

    public FeignMethodReferencesSearcher() {
        super(true);
    }

    @Override
    public void processQuery(@NotNull MethodReferencesSearch.SearchParameters queryParameters,
                             @NotNull Processor<? super PsiReference> consumer) {
        PsiMethod method = queryParameters.getMethod();
        if (!FeignControllerNavigator.isControllerMethod(method)) {
            return;
        }

        List<PsiElement> feignMethods = FeignControllerNavigator.findFeignClients(method);
        for (PsiElement element : feignMethods) {
            if (!(element instanceof PsiMethod) || !element.isValid()) {
                continue;
            }
            PsiMethod feignMethod = (PsiMethod) element;
            PsiIdentifier nameIdentifier = feignMethod.getNameIdentifier();
            if (nameIdentifier == null) {
                continue;
            }
            PsiReference reference = new PsiReferenceBase.Immediate<>(nameIdentifier, method);
            if (!consumer.process(reference)) {
                return;
            }
        }
    }
}
