package com.waaagh.navigation;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Injects the Feign &lt;-&gt; Controller mapping into IDEA's native
 * "Go to Declaration or Usages" popup (Ctrl+B / Ctrl+Click / Show Usages).
 * <p>
 * Historical note: this used to be a {@code gotoDeclarationHandler}, which hijacked Ctrl+B on a
 * method declaration. That extension point can only produce the plain "Go To Declaration" target
 * popup ({@code NavigationActionResult.MultipleTargets} rendered by {@code buildTargetPopup}) — it
 * has no way to return {@code GTDUActionResult.SU}, so the rich usages popup
 * ({@code ShowUsagesAction}/{@code UsageView}: signature header, "Merge Usages from the Same Line"
 * filters, the selected file path at the bottom) could never be shown. {@code ShowUsagesAction}
 * reaches the usages popup through {@code FindUsagesManager#getFindUsagesHandler(element,
 * OperationMode.USAGES_WITH_DEFAULT_OPTIONS)}, so the mapping is contributed there instead — the
 * platform then renders it together with the real call sites and keeps its own chrome.
 * <p>
 * Only {@link OperationMode#USAGES_WITH_DEFAULT_OPTIONS} (the Show Usages popup) is intercepted.
 * Find Usages (Alt+F7, {@link OperationMode#DEFAULT}) and editor highlight-usages
 * ({@link OperationMode#HIGHLIGHT_USAGES}) still get the platform's Java handler, so their results
 * are unchanged.
 */
public final class FeignControllerFindUsagesHandlerFactory extends FindUsagesHandlerFactory {

    @Override
    public boolean canFindUsages(@NotNull PsiElement element) {
        PsiMethod method = FeignControllerNavigator.resolveMethod(element);
        return FeignControllerNavigator.isFeignMethod(method)
                || FeignControllerNavigator.isControllerMethod(method);
    }

    @Override
    public @Nullable FindUsagesHandler createFindUsagesHandler(@NotNull PsiElement element,
                                                              boolean forHighlightUsages) {
        return createFindUsagesHandler(element, forHighlightUsages
                ? OperationMode.HIGHLIGHT_USAGES
                : OperationMode.DEFAULT);
    }

    @Override
    public @Nullable FindUsagesHandler createFindUsagesHandler(@NotNull PsiElement element,
                                                              @NotNull OperationMode operationMode) {
        // Only the Ctrl+B "Go to Declaration or Usages" popup should see the counterpart as a fake
        // usage. Everything else (Find Usages, highlight usages) falls through to the platform's
        // Java handler.
        if (operationMode != OperationMode.USAGES_WITH_DEFAULT_OPTIONS) {
            return null;
        }
        PsiMethod method = FeignControllerNavigator.resolveMethod(element);
        if (method == null) {
            return null;
        }
        boolean feignSide = FeignControllerNavigator.isFeignMethod(method);
        if (!feignSide && !FeignControllerNavigator.isControllerMethod(method)) {
            return null;
        }
        return new WaaaghFindUsagesHandler(method, feignSide);
    }
}
