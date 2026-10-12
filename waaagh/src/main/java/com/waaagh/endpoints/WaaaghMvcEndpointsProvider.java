package com.waaagh.endpoints;

import com.intellij.microservices.endpoints.EndpointType;
import com.intellij.microservices.endpoints.EndpointTypes;
import com.intellij.openapi.project.Project;
import com.waaagh.cache.BilateralCacheManager;
import com.waaagh.constant.RestIcons;
import com.waaagh.entity.HttpMappingInfo;
import com.waaagh.utils.ControllerClassScanUtils;
import java.util.Map;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

/**
 * WAAAGH MVC Controllers：把 Spring MVC Controller 端点提供给 Endpoints 工具窗口，
 * 路径由 WAAAGH 解析（context-path + servlet.path + PathMatchConfigurer 前缀 + 映射）。
 */
public final class WaaaghMvcEndpointsProvider extends AbstractWaaaghEndpointsProvider {

    private static final String CONTROLLER_ANNOTATION = "org.springframework.stereotype.Controller";

    private static final String REST_CONTROLLER_ANNOTATION =
        "org.springframework.web.bind.annotation.RestController";

    @Override
    protected String frameworkId() {
        return "waaagh-mvc";
    }

    @Override
    protected String frameworkTitle() {
        return "Waaagh MVC Controllers";
    }

    @Override
    protected Icon icon() {
        return RestIcons.STATEMENT_LINE_CONTROLLER_ICON;
    }

    @Override
    protected EndpointType type() {
        return EndpointTypes.HTTP_SERVER_TYPE;
    }

    @Override
    protected String[] frameworkClassNames() {
        return new String[]{CONTROLLER_ANNOTATION, REST_CONTROLLER_ANNOTATION};
    }

    @Override
    protected boolean fullyScanned(@NotNull Project project) {
        return BilateralCacheManager.isControllerFullyScanned(project);
    }

    @Override
    protected void scan(@NotNull Project project) {
        ControllerClassScanUtils.scanControllerPaths(project);
    }

    @Override
    protected Map<String, HttpMappingInfo> caches(@NotNull Project project) {
        return BilateralCacheManager.queryControllerCaches(project);
    }
}
