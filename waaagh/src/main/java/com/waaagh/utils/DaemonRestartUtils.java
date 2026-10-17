package com.waaagh.utils;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.lang.reflect.Method;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 重启 Daemon 的小工具：让编辑器里已经显示的 gutter 图标、Call Hierarchy 等立即用新的映射数据重算。
 * <p>
 * 用反射而不是直接调用：{@code DaemonCodeAnalyzer#restart()} 从 2025.3 起被标记为废弃，而替代它的
 * {@code restart(Object reason)} 在更老的版本（本插件 since-build 231）里并不存在——直接引用任一个都会
 * 让字节码在其他版本上不兼容，Marketplace 校验也会把旧方法报成 "deprecated API usage"（1.2.3 的
 * Warnings）。运行期优先用新重载，找不到就回退旧方法；都失败时静默跳过——只是图标要等下一次高亮
 * pass 才刷新，不影响映射数据的正确性。
 */
public final class DaemonRestartUtils {

  private static final Logger LOG = Logger.getInstance(DaemonRestartUtils.class);

  /** 2025.3+：{@code restart(Object reason)}；旧版本没有这个重载时回退到无参方法。 */
  private static final Method RESTART_WITH_REASON = findMethod("restart", Object.class);

  /** 231 ~ 2025.2（2025.3 起废弃，但仍是 no-arg，二进制兼容）。 */
  private static final Method RESTART = RESTART_WITH_REASON != null ? null : findMethod("restart");

  private DaemonRestartUtils() {
  }

  public static void restart(@NotNull Project project) {
    if (project.isDisposed()) {
      return;
    }
    Method restart = RESTART_WITH_REASON != null ? RESTART_WITH_REASON : RESTART;
    if (restart == null) {
      return;
    }
    try {
      if (restart.getParameterCount() == 0) {
        restart.invoke(DaemonCodeAnalyzer.getInstance(project));
      } else {
        restart.invoke(DaemonCodeAnalyzer.getInstance(project), "WAAAGH cache refresh");
      }
    } catch (Throwable t) {
      // 反射失败（方法签名被移除等）：放弃本次立即刷新，不要影响调用方
      LOG.debug("Cannot restart the daemon", t);
    }
  }

  @Nullable
  private static Method findMethod(@NotNull String name, Class<?>... parameterTypes) {
    try {
      return DaemonCodeAnalyzer.class.getMethod(name, parameterTypes);
    } catch (NoSuchMethodException | LinkageError ignored) {
      return null;
    }
  }
}
