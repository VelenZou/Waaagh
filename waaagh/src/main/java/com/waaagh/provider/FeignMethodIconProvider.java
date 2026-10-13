package com.waaagh.provider;

import static com.waaagh.constant.RestIcons.STATEMENT_LINE_FEIGN_ICON;

import com.intellij.ide.IconProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.waaagh.navigation.FeignControllerNavigator;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Carries the WAAAGH Feign jump icon for a Feign endpoint method's <em>name identifier</em>.
 * <p>
 * The icon is intentionally bound to the identifier, not to the Feign interface class: the native
 * "Go to Declaration or Usages" popup renders each row from {@code usage.getElement().getIcon(0)}
 * (see {@code UsageInfo2UsageAdapter.computeIcon}), and the Feign &lt;-&gt; Controller counterparts are
 * injected as usages of the counterpart's name identifier. A class-level override (as used before)
 * also repainted every ordinary usage whose containing class is a Feign / Controller as well as the
 * editor tabs and the project tree, which is not wanted — see {@link MethodIconSupport}.
 */
public class FeignMethodIconProvider extends IconProvider {

    @Override
    public @Nullable Icon getIcon(@NotNull PsiElement element, int flags) {
        PsiMethod method = MethodIconSupport.methodFor(element);
        return method != null && FeignControllerNavigator.isFeignMethod(method)
                ? STATEMENT_LINE_FEIGN_ICON
                : null;
    }
}
