package com.waaagh.listener.app;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.waaagh.cache.BilateralCacheManager;
import com.waaagh.utils.MappingRelevantFiles;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 文件保存 / 外部编辑器（Cursor 等）修改被 IDEA 同步后，清理双边全量扫描缓存；
 * 下次 gutter 图标、跳转或 Call Hierarchy 查询会自动重新扫描，无需重启或重开项目。
 * <p>
 * 只对可能影响映射的文件类型生效（Java 源码、Spring 配置文件、构建描述文件，见
 * {@link MappingRelevantFiles}），避免无关文件变更触发不必要的重扫。
 * 未保存的编辑器内改动（不会产生 VFS 事件）由 {@link ProjectCacheInvalidator} 负责；
 * 单个方法的 Copy URL 本来就是即时重算，不受缓存影响。
 * <p>
 * 通过 {@code VirtualFileManager.VFS_CHANGES} 消息总线 topic 注册（见 plugin.xml 的
 * {@code applicationListeners}）。
 */
public class VfsCacheRefreshListener implements BulkFileListener {

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        String changedFile = firstRelevantFileName(events);
        if (changedFile == null) {
            return;
        }
        // 事件不携带"属于哪个项目"的信息：打开的项目数量很少，直接全部清理。
        // 多余的清理是无害的——只是下次查询多一次扫描。
        // 批量事件只取第一个相关文件名作为日志里的原因（-Dwaaagh.debug=true 时输出）。
        String reason = "vfs save/change: " + changedFile;
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            BilateralCacheManager.clear(project, reason);
        }
    }

    /**
     * 返回第一个与映射相关的事件文件名（delete / move 等 VirtualFile 不可用时退回事件路径）；
     * 与本批事件全部无关时返回 {@code null}。
     */
    @Nullable
    private static String firstRelevantFileName(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            if (!isMappingRelevantEvent(event)) {
                continue;
            }
            VirtualFile file = event.getFile();
            String name = file != null ? file.getName() : event.getPath();
            return name == null ? "unknown" : name;
        }
        return null;
    }

    private static boolean isMappingRelevantEvent(VFileEvent event) {
        VirtualFile file = event.getFile();
        if (file != null) {
            return MappingRelevantFiles.isRelevant(file);
        }
        // delete/move 等事件的 VirtualFile 可能不可用，退回用事件路径判断
        return MappingRelevantFiles.isRelevantName(event.getPath());
    }
}
