package com.waaagh.endpoints;

import com.intellij.microservices.endpoints.EndpointType;
import com.intellij.microservices.endpoints.EndpointTypes;
import com.intellij.openapi.project.Project;
import com.waaagh.cache.BilateralCacheManager;
import com.waaagh.constant.RestIcons;
import com.waaagh.entity.HttpMappingInfo;
import com.waaagh.utils.FeignClassScanUtils;
import java.util.Map;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

/**
 * WAAAGH Feign Client：把 Spring Cloud OpenFeign 客户端接口提供给 Endpoints 工具窗口，
 * 路径由 WAAAGH 解析（@FeignClient path + 方法映射，含父接口继承的端点）。
 */
public final class WaaaghFeignEndpointsProvider extends AbstractWaaaghEndpointsProvider {

    private static final String FEIGN_CLIENT_ANNOTATION =
        "org.springframework.cloud.openfeign.FeignClient";

    @Override
    protected String frameworkId() {
        return "waaagh-feign";
    }

    @Override
    protected String frameworkTitle() {
        return "Waaagh Feign Client";
    }

    @Override
    protected Icon icon() {
        return RestIcons.STATEMENT_LINE_FEIGN_ICON;
    }

    @Override
    protected EndpointType type() {
        return EndpointTypes.HTTP_CLIENT_TYPE;
    }

    @Override
    protected String[] frameworkClassNames() {
        return new String[]{FEIGN_CLIENT_ANNOTATION};
    }

    @Override
    protected boolean fullyScanned(@NotNull Project project) {
        return BilateralCacheManager.isFeignFullyScanned(project);
    }

    @Override
    protected void scan(@NotNull Project project) {
        FeignClassScanUtils.scanFeignInterfaces(project);
    }

    @Override
    protected Map<String, HttpMappingInfo> caches(@NotNull Project project) {
        return BilateralCacheManager.queryFeignCaches(project);
    }
}
