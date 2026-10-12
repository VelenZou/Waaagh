package com.waaagh.navigation;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Powers Ctrl+B / Ctrl+Click navigation between Feign methods and matching Controllers.
 * <p>
 * Only the method <em>declaration</em> itself (the method name or the {@link PsiMethod}) is handled.
 * For anything inside a method body — fields, locals, calls, parameters, annotations — the handler
 * stays silent so IDEA's native Go to Declaration keeps working (e.g. jumping from
 * {@code userService.getUserById()} to the called method instead of the enclosing Controller method).
 * <p>
 * The platform treats "Go to Declaration or Usages" as a replace-only chain: as soon as a
 * {@link GotoDeclarationHandler} returns a non-empty array, the native resolution (which for a method
 * <em>declaration</em> means "show its usages") is skipped. To keep both the Feign &lt;-&gt; Controller
 * mapping and the callers reachable from the same Ctrl+B action, the mapping targets are merged with
 * the declaration's usages (call sites) into a single chooser.
 */
public class FeignGotoDeclarationHandler implements GotoDeclarationHandler {

    /**
     * Upper bound on how many usages are merged into the chooser. The reference search is synchronous
     * and the popup is not virtualized, so cap the list; the full set is still available via Find Usages.
     */
    private static final int MAX_USAGE_TARGETS = 50;

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement,
                                                             int offset,
                                                             Editor editor) {
        PsiMethod method = resolveDeclaredMethod(sourceElement);
        if (method == null) {
            return null;
        }

        List<PsiElement> targets;
        if (FeignControllerNavigator.isFeignMethod(method)) {
            targets = FeignControllerNavigator.findControllers(method);
        } else if (FeignControllerNavigator.isControllerMethod(method)) {
            targets = FeignControllerNavigator.findFeignClients(method);
        } else {
            return null;
        }

        if (targets.isEmpty()) {
            // Nothing mapped on the other side: stay silent so IDEA's native
            // "Go to Declaration or Usages" handles the declaration.
            return null;
        }

        // A non-empty result replaces the native "or Usages" branch, so merge the declaration's
        // call sites back in — otherwise Ctrl+B on a Feign method could no longer return to the
        // code that calls it (the mapping and the callers are two sides of the same relation).
        List<PsiElement> merged = new ArrayList<>(targets);
        appendUsageTargets(method, merged);
        return merged.toArray(PsiElement.EMPTY_ARRAY);
    }

    /**
     * Resolves the method only when the caret sits on its declaration (its name identifier),
     * never when it sits on something inside the method body.
     */
    @Nullable
    private static PsiMethod resolveDeclaredMethod(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }
        if (element instanceof PsiMethod) {
            return (PsiMethod) element;
        }
        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);
        if (method == null) {
            return null;
        }
        PsiElement nameIdentifier = method.getNameIdentifier();
        return nameIdentifier != null && PsiTreeUtil.isAncestor(nameIdentifier, element, false)
                ? method
                : null;
    }

    /**
     * Appends the declaration's usages (exact call sites) to {@code targets}, preserving IDEA's native
     * "Go to Declaration or Usages" content when the mapping targets take over the action.
     * <p>
     * Each usage is the referencing element itself (e.g. the {@code userClient.getUserById} expression),
     * so selecting it lands precisely on the calling code. The chooser renders it as its source text
     * (+ module) rather than with a type icon / container, which is the trade-off for exact navigation.
     */
    private static void appendUsageTargets(@NotNull PsiMethod method, @NotNull List<PsiElement> targets) {
        Project project = method.getProject();
        if (DumbService.isDumb(project)) {
            return;
        }
        // The platform invokes GotoDeclarationHandler with read access already held.
        int[] added = {0};
        ReferencesSearch.search(method).forEach((PsiReference reference) -> {
            if (added[0] >= MAX_USAGE_TARGETS) {
                return false;
            }
            PsiElement usage = reference.getElement();
            // Javadoc {@link ...} are not calls: navigating them lands on the *linked declaration*
            // (i.e. back on the Feign/Controller method), not on any caller code. The platform's own
            // CallerMethodsTreeStructure skips them for the same reason ("javadoc couldn't call anything").
            if (PsiUtil.isInsideJavadocComment(usage)) {
                return true;
            }
            if (usage.isValid() && !targets.contains(usage)) {
                targets.add(usage);
                added[0]++;
            }
            return true;
        });
    }
}
