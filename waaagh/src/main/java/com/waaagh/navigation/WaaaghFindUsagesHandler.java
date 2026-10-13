package com.waaagh.navigation;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Usages handler used only by the "Go to Declaration or Usages" popup (see
 * {@link FeignControllerFindUsagesHandlerFactory}).
 * <p>
 * It extends the base {@link FindUsagesHandler}, whose {@code processElementUsages} is exactly the
 * call-site search ({@code ReferencesSearch}) IDEA's native popup would have run, then appends the
 * matching Feign / Controller counterpart(s) as extra usages of the declaration. Presenting the
 * counterpart as a usage is what lets the native usages popup list the mapping next to the real
 * callers while keeping its own header, filters and status bar.
 */
final class WaaaghFindUsagesHandler extends FindUsagesHandler {

    /** The declaration whose Ctrl+B popup is being built. */
    private final PsiMethod method;

    /** {@code true} when {@link #method} is the Feign method (its mapping target is a Controller). */
    private final boolean feignSide;

    WaaaghFindUsagesHandler(@NotNull PsiMethod method, boolean feignSide) {
        super(method);
        this.method = method;
        this.feignSide = feignSide;
    }

    @Override
    public boolean processElementUsages(@NotNull PsiElement element,
                                        @NotNull Processor<? super UsageInfo> processor,
                                        @NotNull FindUsagesOptions options) {
        // 1. The real call sites, exactly as the native popup would have found them. Javadoc
        //    {@link ...} entries are links, not calls (navigating them lands back on the linked
        //    declaration), so they are filtered out like the platform's CallerMethodsTreeStructure.
        boolean callersProcessed = super.processElementUsages(element, usage -> {
            PsiElement usageElement = usage.getElement();
            if (usageElement != null && PsiUtil.isInsideJavadocComment(usageElement)) {
                return true;
            }
            return processor.process(usage);
        }, options);
        if (!callersProcessed) {
            return false;
        }

        // 2. The Feign <-> Controller counterpart(s), presented as usages so they show up in the
        //    same native popup.
        return processMappingUsages(processor);
    }

    /**
     * Emits one usage per mapping counterpart. The usage search runs on a pooled thread without
     * read access (only the native {@code ReferencesSearch} acquires it on its own), while both the
     * mapper resolution (project file index) and {@link UsageInfo}'s smart-pointer creation need it
     * — so this block is wrapped in a read action rather than looked up eagerly in the factory,
     * which is called on the EDT. {@code super.processElementUsages} is deliberately left outside:
     * it runs a cancellable reference search, whereas this part is short and blocking.
     */
    private boolean processMappingUsages(@NotNull Processor<? super UsageInfo> processor) {
        Computable<Boolean> resolve = () -> {
            if (!method.isValid()) {
                return true;
            }
            List<PsiElement> counterparts = feignSide
                    ? FeignControllerNavigator.findControllers(method)
                    : FeignControllerNavigator.findFeignClients(method);
            for (PsiElement counterpart : counterparts) {
                if (!(counterpart instanceof PsiMethod)) {
                    continue;
                }
                PsiElement anchor = ((PsiMethod) counterpart).getNameIdentifier();
                PsiElement usageElement = anchor != null ? anchor : counterpart;
                if (!processor.process(new UsageInfo(usageElement))) {
                    return false;
                }
            }
            return true;
        };
        return ApplicationManager.getApplication().runReadAction(resolve);
    }
}
