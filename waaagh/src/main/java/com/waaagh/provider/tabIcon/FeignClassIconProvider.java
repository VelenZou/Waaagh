package com.waaagh.provider.tabIcon;

import static com.waaagh.constant.RestIcons.STATEMENT_LINE_FEIGN_ICON;

import com.intellij.ide.IconProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.waaagh.user.UserFeignSettings;
import com.waaagh.utils.AnnotationParserUtils;
import com.waaagh.utils.ProjectUtils;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeignClassIconProvider extends IconProvider {

  @Override
  public @Nullable Icon getIcon(@NotNull PsiElement element, int flags) {
    // 排除三方依赖扫描
    if (!ProjectUtils.isBizElement(element)) {
      return null;
    }

    if (!(element instanceof PsiClass)) {
      return null;
    }

    PsiClass psiClass = (PsiClass) element;
    // 只对接口类做处理
    if (!psiClass.isInterface()) {
      return null;
    }

    // 开启 FeignClient 文件图标
    if (!UserFeignSettings.getInstance().isIconEnabled()) {
      return null;
    }

    if (AnnotationParserUtils.isFeignInterface(psiClass)) {
      return STATEMENT_LINE_FEIGN_ICON;
    }

    return null;
  }
}
