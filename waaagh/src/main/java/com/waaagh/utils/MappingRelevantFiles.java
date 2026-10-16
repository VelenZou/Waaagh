package com.waaagh.utils;

import com.intellij.openapi.vfs.VirtualFile;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * 判断一个文件是否可能影响 Feign ↔ Controller 映射数据。
 * <p>
 * 映射信息只由 Java 注解（Feign / Controller / 路径常量）与 Spring / 构建配置决定：
 * <ul>
 *   <li>Java 源码：内容是否相关由调用方进一步判定（见
 *       {@code ProjectCacheInvalidator}），这里只按扩展名放行；</li>
 *   <li>Spring 配置文件：{@code .properties} / {@code .yml} / {@code .yaml}
 *       （context-path、servlet path、{@code addPathPrefix} 占位符都在这些文件里）；</li>
 *   <li>构建描述文件：{@code pom.xml} / {@code build.gradle(.kts)} /
 *       {@code settings.gradle(.kts)} —— 增删模块、改源根会让"哪些类算业务代码"改变；</li>
 *   <li>其余文件（README、图片、日志、{@code .http} 等）：与映射无关，不应触发重扫。</li>
 * </ul>
 */
public final class MappingRelevantFiles {

  /** 构建描述文件：只按文件名精确匹配，避免把项目里所有 .xml（日志、报表等）都算成相关文件。 */
  private static final Set<String> BUILD_FILE_NAMES = new HashSet<>(Arrays.asList(
      "pom.xml",
      "build.gradle",
      "build.gradle.kts",
      "settings.gradle",
      "settings.gradle.kts"));

  private MappingRelevantFiles() {
  }

  /**
   * 按文件名判断是否可能影响映射；delete / move 等拿不到 {@link VirtualFile} 的事件退回用事件路径，
   * 因此这里也接受完整路径（取最后一段作为文件名）。
   */
  public static boolean isRelevantName(@Nullable String fileNameOrPath) {
    if (fileNameOrPath == null) {
      return false;
    }
    String lower = fileNameOf(fileNameOrPath).toLowerCase(Locale.ROOT);
    return lower.endsWith(".java")
        || lower.endsWith(".properties")
        || lower.endsWith(".yml")
        || lower.endsWith(".yaml")
        || BUILD_FILE_NAMES.contains(lower);
  }

  /**
   * 映射信息只由 Java 注解与 Spring / 构建配置决定，其余文件（目录、README、图片、构建产物等）无需清理。
   */
  public static boolean isRelevant(@Nullable VirtualFile file) {
    return file != null && !file.isDirectory() && isRelevantName(file.getName());
  }

  /** 是否为 Java 源文件（非 Java 的相关文件一律按"内容相关"处理）。 */
  public static boolean isJavaName(@Nullable String fileNameOrPath) {
    return fileNameOrPath != null
        && fileNameOf(fileNameOrPath).toLowerCase(Locale.ROOT).endsWith(".java");
  }

  private static String fileNameOf(String fileNameOrPath) {
    int slash = Math.max(fileNameOrPath.lastIndexOf('/'), fileNameOrPath.lastIndexOf('\\'));
    return slash >= 0 ? fileNameOrPath.substring(slash + 1) : fileNameOrPath;
  }
}
