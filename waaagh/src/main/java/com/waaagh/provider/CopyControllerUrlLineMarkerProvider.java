package com.waaagh.provider;

import static com.waaagh.constant.RestIcons.STATEMENT_LINE_CLIPBOARD_CONTROLLER_ICON;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.GutterName;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.waaagh.cache.BilateralCacheManager;
import com.waaagh.constant.RestIcons;
import com.waaagh.entity.HttpMappingInfo;
import com.waaagh.utils.AnnotationParserUtils;
import com.waaagh.utils.ControllerClassScanUtils;
import com.waaagh.utils.ProjectUtils;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class CopyControllerUrlLineMarkerProvider extends LineMarkerProviderDescriptor {

  @Override
  public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
    // 排除三方依赖扫描
    if (!ProjectUtils.isBizElement(element)) {
      return null;
    }

    Project project = element.getProject();
    if (DumbService.isDumb(project)) {
      return null; // 索引未完成，跳过
    }

    if (!(element instanceof PsiMethod)) {
      return null;
    }

    PsiMethod method = (PsiMethod) element;
    if (!method.isValid()) {
      return null;
    }

    if (!AnnotationParserUtils.isElementWithinController(method)) {
      return null;
    }

    // 解析 restfull 注解，下面 gutter 挂在注解旁
    PsiAnnotation restfulAnnotation = AnnotationParserUtils.findRestfulAnnotation(method);
    if (Objects.isNull(restfulAnnotation)) {
      return null;
    }

    // 初始化所有的 controller 缓存
    ControllerClassScanUtils.scanControllerPaths(method.getProject());

    HttpMappingInfo controllerCache = BilateralCacheManager.setOrCoverControllerCache(method);
    if (Objects.isNull(controllerCache)) {
      return null;
    }

    String url = controllerCache.getPath();
    if (StringUtils.isBlank(url)) {
      return null;
    }

    PsiMethod finalMethod = method;
    GutterIconNavigationHandler<PsiElement> handler = (mouseEvent, elt) -> {
      CopyPasteManager.getInstance().setContents(new StringSelection(url));
      NotificationGroupManager.getInstance()
          .getNotificationGroup("Waaagh")
          .createNotification("URL Copied To Clipboard:\n" + url, NotificationType.INFORMATION)
          .notify(finalMethod.getProject());
    };

    // 构建图标信息，挂在方法上
    LineMarkerInfo<PsiElement> marker = new LineMarkerInfo<>(
        restfulAnnotation,
        restfulAnnotation.getTextRange(),
        STATEMENT_LINE_CLIPBOARD_CONTROLLER_ICON,
        psi -> "Click To Copy Controller-URL: " + url,
        handler,
        GutterIconRenderer.Alignment.RIGHT,
        () -> "Copy Controller URL"
    );

    return marker;
  }

  @Override
  public @Nullable("null means disabled") @GutterName String getName() {
    return "Copy Controller URL";
  }
}
