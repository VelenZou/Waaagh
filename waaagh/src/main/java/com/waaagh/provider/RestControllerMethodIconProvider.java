package com.waaagh.provider;

import static com.waaagh.constant.RestIcons.STATEMENT_LINE_CONTROLLER_ICON;

import com.intellij.ide.IconProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.waaagh.navigation.FeignControllerNavigator;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Carries the WAAAGH Controller jump icon for a Controller endpoint method's <em>name identifier</em>.
 * <p>
 * Mirrors {@link FeignMethodIconProvider}: the icon is bound to the identifier (the element the
 * injected usages point at) rather than to the {@code @RestController} class, so native usages and
 * the editor tab / project tree keep IDEA's own icons.
 */
public class RestControllerMethodIconProvider extends IconProvider {

    @Override
    public @Nullable Icon getIcon(@NotNull PsiElement element, int flags) {
        PsiMethod method = MethodIconSupport.methodFor(element);
        return method != null && FeignControllerNavigator.isControllerMethod(method)
                ? STATEMENT_LINE_CONTROLLER_ICON
                : null;
    }
}
