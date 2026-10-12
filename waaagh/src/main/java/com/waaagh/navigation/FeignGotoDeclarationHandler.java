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
import com.waaagh.constant.RestIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;

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
 * the declaration's usages (call sites) into a single chooser; see {@link GotoTargetElement} for how
 * the targets are presented (WAAAGH jump icons per side, code lines and locations for call sites).
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

        boolean toControllers = FeignControllerNavigator.isFeignMethod(method);
        List<PsiElement> mapping;
        Icon sideIcon;
        if (toControllers) {
            mapping = FeignControllerNavigator.findControllers(method);
            sideIcon = RestIcons.STATEMENT_LINE_CONTROLLER_ICON;
        } else if (FeignControllerNavigator.isControllerMethod(method)) {
            mapping = FeignControllerNavigator.findFeignClients(method);
            sideIcon = RestIcons.STATEMENT_LINE_FEIGN_ICON;
        } else {
            return null;
        }

        if (mapping.isEmpty()) {
            // Nothing mapped on the other side: stay silent so IDEA's native
            // "Go to Declaration or Usages" handles the declaration.
            return null;
        }

        // Present each side with its own WAAAGH jump icon: blue for the matching Controllers,
        // orange for the matching Feign clients (the chooser takes the icon from the element).
        List<PsiElement> targets = new ArrayList<>(mapping.size());
        for (PsiElement target : mapping) {
            targets.add(target instanceof PsiMethod
                    ? GotoTargetElement.forMapping((PsiMethod) target, sideIcon)
                    : target);
        }

        // A non-empty result replaces the native "or Usages" branch, so merge the declaration's
        // call sites back in — otherwise Ctrl+B on a Feign method could no longer return to the
        // code that calls it (the mapping and the callers are two sides of the same relation).
        appendUsageTargets(method, targets);
        return targets.toArray(PsiElement.EMPTY_ARRAY);
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
     * Each usage is wrapped in a {@link GotoTargetElement}: the chooser renders every row from the
     * element's own presentation, and a bare usage element has none — it would show as its raw source
     * text, without icon or location. The wrapper still navigates exactly to the call site, but
     * presents it as the caller's code line with the caller's icon and an
     * {@code in Class.method, File.java:42} location (the native "Show Usages" look).
     */
    private static void appendUsageTargets(@NotNull PsiMethod method, @NotNull List<PsiElement> targets) {
        Project project = method.getProject();
        if (DumbService.isDumb(project)) {
            return;
        }
        // The platform invokes GotoDeclarationHandler with read access already held.
        Set<PsiElement> seen = new HashSet<>();
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
            if (usage.isValid() && seen.add(usage)) {
                targets.add(GotoTargetElement.forUsage(usage));
                added[0]++;
            }
            return true;
        });
    }
}
