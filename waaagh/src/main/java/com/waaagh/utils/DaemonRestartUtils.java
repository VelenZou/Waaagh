package com.waaagh.utils;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * 重启 Daemon 的小工具：让编辑器里已经显示的 gutter 图标、Call Hierarchy 等立即用新的映射数据重算。
 * <p>
 * 这里固定使用 no-arg {@code DaemonCodeAnalyzer#restart()}：2026.2 起它被标记为
 * {@code @Deprecated}，但新的 {@code restart(Object)} / {@code restart(PsiFile, Object)} 重载在
 * 231（本插件 since-build）上的参数类型是 {@code Disposable}，调用点字节码不兼容，
 * 因此为了兼容 231 只能继续用旧方法。
 */
public final class DaemonRestartUtils {

  private DaemonRestartUtils() {
  }

  @SuppressWarnings("deprecation")
  public static void restart(@NotNull Project project) {
    if (!project.isDisposed()) {
      DaemonCodeAnalyzer.getInstance(project).restart();
    }
  }
}
