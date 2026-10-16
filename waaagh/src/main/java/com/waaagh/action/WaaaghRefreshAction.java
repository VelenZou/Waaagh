package com.waaagh.action;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.waaagh.cache.BilateralCacheManager;
import com.waaagh.utils.ControllerClassScanUtils;
import com.waaagh.utils.DaemonRestartUtils;
import com.waaagh.utils.FeignClassScanUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 手动刷新映射：清空缓存并在后台重扫，最后重启 Daemon 让 gutter 图标、Call Hierarchy 与
 * Endpoints 视图立即拿到最新数据。
 * <p>
 * 自动失效已覆盖：文件保存 / 外部修改、编辑器内未保存的 PSI 编辑（停顿约 500ms 后清理）、
 * 索引完成与模块 / 源根变化；这个 action 是怀疑缓存与代码不一致时的兜底入口，
 * 也可通过 Find Action 搜索 "Refresh Feign/Controller Mappings" 执行。
 */
public class WaaaghRefreshAction extends DumbAwareAction {

  private static final String NOTIFICATION_GROUP = "Waaagh";

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    Project project = event.getProject();
    if (project == null || project.isDisposed()) {
      return;
    }
    if (DumbService.isDumb(project)) {
      notifyUser(project,
          "Indexing is in progress — the Feign/Controller mappings can be refreshed once it finishes.",
          NotificationType.WARNING);
      return;
    }
    // 先清缓存：Endpoints 视图订阅了数据版本，会跟着重载
    BilateralCacheManager.clear(project, "manual refresh");
    // 后台重扫一次，避免下一次 gutter / 导航请求才发现要重扫；扫描结果同时用于反馈
    new Task.Backgroundable(project, "Refreshing Feign/Controller mappings", false) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        if (project.isDisposed()) {
          return;
        }
        int[] counts = ApplicationManager.getApplication().runReadAction(
            (Computable<int[]>) () -> new int[]{
                ControllerClassScanUtils.scanControllerPaths(project).size(),
                FeignClassScanUtils.scanFeignInterfaces(project).size()});
        ApplicationManager.getApplication().invokeLater(() -> {
          if (project.isDisposed()) {
            return;
          }
          // 重启 Daemon：编辑器里已显示的 gutter 图标 / Call Hierarchy 等随之重算
          // （DaemonRestartUtils 里固定用 no-arg restart()，兼容 since-build 231）
          DaemonRestartUtils.restart(project);
          notifyUser(project,
              "WAAAGH mappings refreshed: " + counts[0] + " Controller and " + counts[1]
                  + " Feign endpoints.",
              NotificationType.INFORMATION);
        });
      }
    }.queue();
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    // 无项目时（如欢迎界面）不显示；项目内始终可用（Dumb 模式下给出提示）
    event.getPresentation().setEnabledAndVisible(event.getProject() != null);
  }

  private static void notifyUser(@NotNull Project project, @NotNull String content,
      @NotNull NotificationType type) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup(NOTIFICATION_GROUP)
        .createNotification(content, type)
        .notify(project);
  }
}
