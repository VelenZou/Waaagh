package com.waaagh.endpoints;

import com.intellij.microservices.url.Authority;
import com.intellij.microservices.url.UrlConstants;
import com.intellij.microservices.url.UrlPath;
import com.intellij.microservices.url.UrlTargetInfo;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.waaagh.constant.RestIcons;
import com.waaagh.entity.HttpMappingInfo;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * WAAAGH 端点条目的 URL 目标模型。
 * <p>
 * Endpoints 工具窗口、HTTP Client 请求生成、OpenAPI / Examples 侧栏都会通过
 * {@link UrlTargetInfo} 取地址，因此这里的 path 就是各处的唯一来源。
 */
final class WaaaghUrlTargetInfo implements UrlTargetInfo {

    private final List<String> schemes;

    private final UrlPath path;

    private final List<Authority> authorities;

    private final HttpMappingInfo mapping;

    private final Set<String> methods;

    private final boolean deprecated;

    WaaaghUrlTargetInfo(@NotNull UrlPath path,
                        @NotNull List<Authority> authorities,
                        @NotNull HttpMappingInfo mapping) {
        this.schemes = UrlConstants.HTTP_SCHEMES;
        this.path = path;
        this.authorities = authorities;
        this.mapping = mapping;
        Set<String> requestMethods = mapping.getRequestMethods();
        this.methods = requestMethods == null || requestMethods.isEmpty()
            ? Collections.emptySet()
            : Collections.unmodifiableSet(new LinkedHashSet<>(requestMethods));
        this.deprecated = WaaaghEndpointsUtil.isDeprecated(mapping.getPsiMethod());
    }

    @Override
    public List<String> getSchemes() {
        return schemes;
    }

    @Override
    public List<Authority> getAuthorities() {
        return authorities;
    }

    @Override
    public UrlPath getPath() {
        return path;
    }

    @Override
    public Icon getIcon() {
        return RestIcons.WAAAGH_ENDPOINT_ICON;
    }

    @Override
    public boolean isDeprecated() {
        return deprecated;
    }

    @Override
    public PsiElement resolveToPsiElement() {
        return mapping.getPsiMethod();
    }

    @Override
    public Set<String> getMethods() {
        return methods;
    }

    @Nullable
    @Override
    public PsiElement getDocumentationPsiElement() {
        PsiMethod method = mapping.getPsiMethod();
        return method == null ? null : method;
    }
}
