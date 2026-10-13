package com.waaagh.provider;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared lookup for the WAAAGH method icons (see {@link FeignMethodIconProvider}).
 */
final class MethodIconSupport {

    private MethodIconSupport() {
    }

    /**
     * Resolves the method to decorate for the given element.
     * <p>
     * Both shapes are needed:
     * <ul>
     *   <li>the method itself — the "Go to Implementation" popup (Ctrl+Alt+B) renders each entry
     *       through {@code JavaGotoTargetPresentationProvider} → {@code PsiElementRenderingInfo},
     *       which reads its icon from {@link PsiMethod#getIcon(int)};</li>
     *   <li>the method <em>name identifier</em> — the Feign &lt;-&gt; Controller mapping entries in the
     *       "Go to Declaration or Usages" popup (Ctrl+B) are injected as usages of the counterpart's
     *       name identifier, and that popup reads {@code usage.getElement().getIcon(0)}.</li>
     * </ul>
     */
    static @Nullable PsiMethod methodFor(@NotNull PsiElement element) {
        if (element instanceof PsiMethod) {
            return (PsiMethod) element;
        }
        if (!(element instanceof PsiIdentifier)) {
            return null;
        }
        PsiElement parent = element.getParent();
        if (!(parent instanceof PsiMethod)) {
            return null;
        }
        PsiMethod method = (PsiMethod) parent;
        return method.getNameIdentifier() == element ? method : null;
    }
}
