package com.waaagh.listener.app;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.waaagh.cache.BilateralCacheManager;
import org.jetbrains.annotations.NotNull;

/**
 * 当前项目关闭，缓存清理。
 * <p>
 * 通过 {@code com.intellij.openapi.project.ProjectManagerListener} 消息总线 topic 注册（plugin.xml
 * 的 {@code applicationListeners}）。
 */
public class CacheCleanListener implements ProjectManagerListener {

    @Override
    public void projectClosing(@NotNull Project project) {
        // 清理双边接口方法缓存
        BilateralCacheManager.clear(project);
    }
}
