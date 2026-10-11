package com.waaagh.endpoints;

import com.intellij.microservices.url.Authority;
import com.intellij.microservices.url.UrlPath;
import com.intellij.microservices.url.UrlTargetInfo;
import com.intellij.microservices.url.references.DefaultFrameworkUrlPathSpecification;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.waaagh.entity.HttpMappingInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * WAAAGH 端点增强（Endpoints 工具窗口）共用工具：
 * URL 规范化、URL 模型构建、废弃标注判断。
 */
final class WaaaghEndpointsUtil {

    private WaaaghEndpointsUtil() {
    }

    /**
     * 规范化 WAAAGH 解析出的完整路径，保证以 "/" 开头，
     * 并保证展示文本与 {@link UrlPath} 使用同一个字符串（搜索、复制、请求生成保持一致）。
     */
    @NotNull
    static String normalizeUrl(@Nullable String rawPath) {
        if (rawPath == null || rawPath.trim().isEmpty()) {
            return "/";
        }
        String url = rawPath.trim();
        return url.startsWith("/") ? url : "/" + url;
    }

    /**
     * 解析路径为 URL 模型（支持 {var} 路径变量）；解析失败时退化为纯文本分段。
     */
    @NotNull
    static UrlPath parsePath(@NotNull String url) {
        try {
            return DefaultFrameworkUrlPathSpecification.INSTANCE.parsePath(url);
        } catch (Throwable ignored) {
            return UrlPath.fromExactString(url);
        }
    }

    /**
     * 构建 URL 目标：scheme 与 Spring 端点保持一致（http/https），
     * authorities 在未知运行端口时退化为 localhost / 127.0.0.1（与 Spring 插件同一策略），
     * 保证 HTTP Client 请求生成的地址与条目地址一致。
     */
    @NotNull
    static List<Authority> authorities() {
        List<Authority> authorities = new ArrayList<>();
        authorities.add(new Authority.Placeholder());
        authorities.add(new Authority.Exact("localhost"));
        authorities.add(new Authority.Exact("127.0.0.1"));
        return authorities;
    }

    @NotNull
    static UrlTargetInfo createUrlTargetInfo(@NotNull HttpMappingInfo endpoint) {
        String url = normalizeUrl(endpoint.getPath());
        return new WaaaghUrlTargetInfo(parsePath(url), authorities(), endpoint);
    }

    /**
     * 方法或所在类标注了 {@code @Deprecated} 时，端点条目按废弃样式展示。
     */
    static boolean isDeprecated(@Nullable PsiMethod method) {
        if (method == null || !method.isValid()) {
            return false;
        }
        if (method.hasAnnotation(CommonNames.DEPRECATED_ANNOTATION)) {
            return true;
        }
        PsiClass containingClass = method.getContainingClass();
        return containingClass != null && containingClass.isValid()
            && containingClass.hasAnnotation(CommonNames.DEPRECATED_ANNOTATION);
    }

    @Nullable
    static PsiFile containingFile(@Nullable PsiElement element) {
        return element == null ? null : element.getContainingFile();
    }

    /** PSI 元素是否可用于端点导航（仍有效且解析到当前 PSI 快照）。 */
    static boolean isUsable(@Nullable PsiElement element) {
        return element != null && element.isValid() && !Objects.isNull(element.getContainingFile());
    }

    /** 常用注解全限定名。 */
    static final class CommonNames {

        static final String DEPRECATED_ANNOTATION = "java.lang.Deprecated";

        private CommonNames() {
        }
    }
}
