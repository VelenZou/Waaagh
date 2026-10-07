package com.waaagh.listener.app;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.waaagh.cache.BilateralCacheManager;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;

/**
 * 文件保存 / 外部编辑器（Cursor 等）修改被 IDEA 同步后，清理双边全量扫描缓存；
 * 下次 gutter 图标、跳转或 Call Hierarchy 查询会自动重新扫描，无需重启或重开项目。
 * <p>
 * 只对可能影响映射的文件类型生效（Java 源码与 Spring 配置文件），避免无关文件变更
 * 触发不必要的重扫。单个方法的 Copy URL 本来就是即时重算，不受缓存影响。
 * <p>
 * 通过 {@code VirtualFileManager.VFS_CHANGES} 消息总线 topic 注册（见 plugin.xml 的
 * {@code applicationListeners}）。
 */
public class VfsCacheRefreshListener implements BulkFileListener {

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        boolean relevant = false;
        for (VFileEvent event : events) {
            if (isMappingRelevantEvent(event)) {
                relevant = true;
                break;
            }
        }
        if (!relevant) {
            return;
        }
        // 事件不携带"属于哪个项目"的信息：打开的项目数量很少，直接全部清理。
        // 多余的清理是无害的——只是下次查询多一次扫描。
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            BilateralCacheManager.clear(project);
        }
    }

    /**
     * 映射信息只由 Java 注解与 Spring 配置决定，其余文件（README、图片、构建产物等）无需清理。
     */
    private static boolean isMappingRelevantEvent(VFileEvent event) {
        VirtualFile file = event.getFile();
        if (file != null && file.isDirectory()) {
            return false;
        }
        // delete/move 等事件的 VirtualFile 可能不可用，退回用事件路径判断
        String name = file != null ? file.getName() : event.getPath();
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".java")
                || lower.endsWith(".properties")
                || lower.endsWith(".yml")
                || lower.endsWith(".yaml");
    }
}
