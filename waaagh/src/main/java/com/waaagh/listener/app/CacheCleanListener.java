package com.waaagh.listener.app;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.waaagh.cache.BilateralCacheManager;
import org.jetbrains.annotations.NotNull;

/**
 * 项目生命周期上的缓存处理：
 * <ul>
 *   <li>项目关闭：清理双边接口方法缓存；</li>
 *   <li>项目已关闭：连数据版本号一起移除，避免同一 IDE 会话里反复开关项目时残留。</li>
 * </ul>
 * 项目打开时的监听安装是惰性的：{@link ProjectCacheInvalidator} 在缓存第一次被填充时注册
 * （见 {@code ControllerClassScanUtils#scanControllerPaths}）。
 * <p>
 * 通过 {@code com.intellij.openapi.project.ProjectManagerListener} 消息总线 topic 注册（plugin.xml
 * 的 {@code applicationListeners}）。
 */
public class CacheCleanListener implements ProjectManagerListener {

    @Override
    public void projectClosing(@NotNull Project project) {
        // 清理双边接口方法缓存
        BilateralCacheManager.clear(project, "project closing");
    }

    @Override
    public void projectClosed(@NotNull Project project) {
        BilateralCacheManager.forget(project);
    }
}
