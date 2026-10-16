package com.waaagh.listener.app;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.util.Alarm;
import com.waaagh.cache.BilateralCacheManager;
import com.waaagh.utils.AnnotationParserUtils;
import com.waaagh.utils.DaemonRestartUtils;
import com.waaagh.utils.MappingRelevantFiles;
import com.waaagh.utils.ProjectUtils;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;

/**
 * 映射缓存的"主动失效"监听（项目级服务），补齐原先只有"保存 / 外部同步"才会清理的场景：
 * <ul>
 *   <li><b>PSI 编辑</b>：改注解 / 加方法 / 改路径后，即使文件还没保存（没有 VFS 事件），
 *       停下编辑约 {@value #REFRESH_DELAY_MS} ms 也会清理缓存，让 gutter 图标、Ctrl+B、
 *       Call Hierarchy、Endpoints 视图拿到新数据。只对可能影响映射的文件生效：
 *       Java 文件里声明了 Controller / FeignClient / 映射注解、缓存里还留着该文件的端点，
 *       或文件里注册了 {@code addPathPrefix}；Spring 配置文件与构建描述文件一律生效；
 *       {@code .http} 等无关文件（HTTP Client 面板）不会触发，避免 Endpoints 视图抖动。</li>
 *   <li><b>索引完成</b>：{@code exitDumbMode} 时清理（Maven / Gradle reload、依赖变化、
 *       Reload Project from Disk、Invalidate Caches 等索引重建场景）。索引期间不清理，
 *       否则会跟着重扫。{@link BilateralCacheManager} 里还记着模块根版本号做惰性兜底。</li>
 *   <li><b>模块 / 源根变化</b>：{@code ModuleRootListener} 清理（新增 / 移除模块、
 *       Mark as Sources Root、SDK 变化等——这些操作本身不产生源码文件的 VFS 事件）。</li>
 * </ul>
 * 这里只负责"清理缓存"：真正的重扫是惰性的，发生在下一次 gutter / 导航 / Endpoints 查询时，
 * 因此不会在用户每次按键时触发全量扫描，也不会为了刷新图标主动重启 Daemon。
 * <p>
 * 项目关闭时随项目一起 {@link #dispose()}，所有监听随之注销。
 */
@Service(Service.Level.PROJECT)
public final class ProjectCacheInvalidator extends PsiTreeChangeAdapter implements Disposable {

  /** PSI 变化后的防抖时长：连续打字只在停顿后清理一次，避免每敲一个字符就清缓存。 */
  private static final int REFRESH_DELAY_MS = 500;

  private final Project project;

  private final Alarm refreshAlarm;

  /** 本次防抖窗口内发生过变化、且可能影响映射的文件。 */
  private final Set<VirtualFile> changedFiles = ConcurrentHashMap.newKeySet();

  /** 无法在防抖回调里判定内容相关性的变化（文件 / 目录重命名、移动等）：保守清理。 */
  private final AtomicBoolean forceClear = new AtomicBoolean();

  public ProjectCacheInvalidator(Project project) {
    this.project = project;
    this.refreshAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
    PsiManager.getInstance(project).addPsiTreeChangeListener(this, this);
    // 旧平台里 MessageBus#subscribe 返回 MessageBus，新平台返回 void：分两句写，避免版本差异
    project.getMessageBus().connect(this)
        .subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
          @Override
          public void exitDumbMode() {
            // 索引重建完成：旧缓存里的路径可能来自旧索引 / 旧 PSI，直接清理并在下次查询时重扫
            invalidateNow("indexing finished");
          }
        });
    project.getMessageBus().connect(this)
        .subscribe(ModuleRootListener.TOPIC, new ModuleRootListener() {
          @Override
          public void rootsChanged(@NotNull ModuleRootEvent event) {
            // 模块、源根、SDK 变化：缓存里的“业务类”判断与端点集合都可能已过期
            invalidateNow("module / source-root change");
          }
        });
  }

  /**
   * 确保监听已安装（幂等）：缓存第一次被填充时调用。
   * <p>
   * 用惰性安装而不是 {@code projectOpened}：只有真正用过映射数据（扫描过）的项目才需要失效监听，
   * 也避开旧平台里已废弃的 ProjectManagerListener#projectOpened。
   */
  public static void ensureInstalled(@NotNull Project project) {
    if (!project.isDisposed()) {
      project.getService(ProjectCacheInvalidator.class);
    }
  }

  // ------------------------------------------------------------------
  // PSI 变化：防抖后清理
  // ------------------------------------------------------------------

  @Override
  public void childAdded(@NotNull PsiTreeChangeEvent event) {
    onChange(event);
  }

  @Override
  public void childRemoved(@NotNull PsiTreeChangeEvent event) {
    onChange(event);
  }

  @Override
  public void childReplaced(@NotNull PsiTreeChangeEvent event) {
    onChange(event);
  }

  @Override
  public void childMoved(@NotNull PsiTreeChangeEvent event) {
    onChange(event);
  }

  @Override
  public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
    onChange(event);
  }

  @Override
  public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
    onChange(event);
  }

  private void onChange(@NotNull PsiTreeChangeEvent event) {
    if (project.isDisposed() || DumbService.isDumb(project)) {
      // 索引期间事件量很大且内容不可信；索引结束（exitDumbMode）时会统一清理
      return;
    }
    String propertyName = event.getPropertyName();
    if (PsiTreeChangeEvent.PROP_FILE_NAME.equals(propertyName)
        || PsiTreeChangeEvent.PROP_DIRECTORY_NAME.equals(propertyName)) {
      // 重命名 / 移动：防抖回调里已经拿不到"旧内容"来判定相关性，保守清理
      forceClear.set(true);
    } else {
      VirtualFile file = eventFile(event);
      if (file == null) {
        forceClear.set(true);
      } else {
        changedFiles.add(file);
      }
    }
    scheduleRefresh();
  }

  private void scheduleRefresh() {
    refreshAlarm.cancelAllRequests();
    refreshAlarm.addRequest(this::refreshIfNeeded, REFRESH_DELAY_MS);
  }

  private void refreshIfNeeded() {
    if (project.isDisposed()) {
      return;
    }
    Set<VirtualFile> files = new HashSet<>(changedFiles);
    changedFiles.clear();
    boolean force = forceClear.getAndSet(false);
    if (!force && files.isEmpty()) {
      return;
    }
    if (DumbService.isDumb(project)) {
      // 索引重建中：PSI / 索引不可信，等 exitDumbMode 统一清理
      return;
    }
    // 重命名 / 移动 / 删除无法在防抖回调里判定相关性，保守清理
    String reason = force ? "editor rename/move" : null;
    if (reason == null && anyFileMayAffectMappings(files)) {
      reason = "editor change (unsaved): " + describeFiles(files);
    }
    if (reason == null) {
      return;
    }
    if (BilateralCacheManager.clear(project, reason)) {
      // 清掉的是真实数据：重启 Daemon，让编辑器里已经显示的 gutter 图标等立即按新映射重算。
      // 不重启的话要等下一次高亮 pass（下次输入 / 切文件）才会更新，用户停下编辑就会看到旧图标。
      ApplicationManager.getApplication().invokeLater(() -> DaemonRestartUtils.restart(project));
    }
  }

  /**
   * 日志用的文件描述：第一个文件名 + 其余数量（{@code -Dwaaagh.debug=true} 时输出）。
   */
  private static String describeFiles(@NotNull Set<VirtualFile> files) {
    StringBuilder description = new StringBuilder();
    int index = 0;
    for (VirtualFile file : files) {
      if (index == 0) {
        description.append(file.getName());
      }
      index++;
    }
    if (index > 1) {
      description.append(" (+").append(index - 1).append(" more)");
    }
    return description.toString();
  }

  /**
   * 判断这批文件里是否有"可能影响映射"的变化；必须持有读锁。
   */
  private boolean anyFileMayAffectMappings(@NotNull Set<VirtualFile> files) {
    try {
      // 用 Application#runReadAction(Computable) 而不是 ReadAction#compute：后者在新平台已废弃，
      // 而它的替代（computeBlocking 等）在 231 上还不存在，插件要保持 since-build 231 兼容。
      Boolean relevant = ApplicationManager.getApplication().runReadAction(
          (Computable<Boolean>) () -> {
            if (project.isDisposed()) {
              return false;
            }
            for (VirtualFile file : files) {
              if (fileMayAffectMappings(file)) {
                return true;
              }
            }
            return false;
          });
      return Boolean.TRUE.equals(relevant);
    } catch (IndexNotReadyException e) {
      // 索引刚好在中途重建：保守清理，重扫由下次查询触发
      return true;
    }
  }

  private boolean fileMayAffectMappings(@NotNull VirtualFile file) {
    String fileName = file.getName();
    if (!MappingRelevantFiles.isRelevantName(fileName)) {
      return false;
    }
    if (!MappingRelevantFiles.isJavaName(fileName)) {
      // Spring 配置文件 / 构建描述文件：任何内容变化都可能影响路径或模块结构
      return true;
    }
    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    if (psiFile == null) {
      return true; // 文件已删除：无法判定，保守清理
    }
    if (!ProjectUtils.isBizElement(psiFile)) {
      return false; // 三方库 / 源码根之外：本来就不会进入映射
    }
    return declaresOrHoldsMappings(psiFile);
  }

  private boolean declaresOrHoldsMappings(@NotNull PsiFile psiFile) {
    // 1) 缓存里还留着这个文件声明的端点（例如刚删掉方法 / 注解，或改了路径）
    if (BilateralCacheManager.hasCachedEntriesIn(psiFile)) {
      return true;
    }
    if (!(psiFile instanceof PsiJavaFile)) {
      return true;
    }
    // 2) 文件里声明了 Controller / FeignClient，或带映射注解的方法（可能是刚写上的注解）
    for (PsiClass psiClass : ((PsiJavaFile) psiFile).getClasses()) {
      if (AnnotationParserUtils.isControllerClass(psiClass)
          || AnnotationParserUtils.isFeignInterface(psiClass)) {
        return true;
      }
      for (PsiMethod method : psiClass.getMethods()) {
        if (AnnotationParserUtils.containsRestfulAnnotation(method)) {
          return true;
        }
      }
    }
    // 3) 文件里注册了包级路径前缀（PathMatchConfigurer#addPathPrefix）：修改后会改变所有端点路径
    return containsAddPathPrefix(psiFile);
  }

  private static boolean containsAddPathPrefix(@NotNull PsiFile psiFile) {
    boolean[] found = {false};
    PsiSearchHelper.getInstance(psiFile.getProject()).processElementsWithWord(
        (element, offsetInElement) -> {
          found[0] = true;
          return false;
        },
        GlobalSearchScope.fileScope(psiFile),
        "addPathPrefix",
        UsageSearchContext.IN_CODE,
        true);
    return found[0];
  }

  // ------------------------------------------------------------------
  // 公共逻辑
  // ------------------------------------------------------------------

  /**
   * 立即清理缓存（索引完成 / 模块结构变化等无需防抖的场景）。
   */
  private void invalidateNow(@NotNull String reason) {
    if (project.isDisposed()) {
      return;
    }
    BilateralCacheManager.clear(project, reason);
  }

  /**
   * 取出事件关联的文件：重命名等事件没有 file，退回用事件元素反查。
   */
  private static VirtualFile eventFile(@NotNull PsiTreeChangeEvent event) {
    PsiFile file = event.getFile();
    if (file == null) {
      file = firstFileOf(event.getParent());
    }
    if (file == null) {
      file = firstFileOf(event.getChild());
    }
    if (file == null) {
      file = firstFileOf(event.getOldChild());
    }
    if (file == null) {
      file = firstFileOf(event.getNewChild());
    }
    return file == null ? null : file.getVirtualFile();
  }

  private static PsiFile firstFileOf(PsiElement element) {
    if (element == null) {
      return null;
    }
    if (element instanceof PsiFile) {
      return (PsiFile) element;
    }
    return element.getContainingFile();
  }

  @Override
  public void dispose() {
    changedFiles.clear();
    forceClear.set(false);
  }
}
