package org.waaagh.cloudfeignserver.controller.v2beta;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 包名前缀的边界用例：本包名 controller.v2beta 以 controller.v2 开头。
 * <p>
 * 规则一用的是 {@code startsWith("org.waaagh.cloudfeignserver.controller.v2")}，
 * 因此本类同样命中前缀 /v2（与 Spring 行为一致，插件不做“包边界”特殊处理）。
 * <p>
 * 完整路径 GET /hello/world/v2/v2beta-api/status → 匹配 V2BetaClient#status。
 */
@RestController
@RequestMapping("/v2beta-api")
public class V2BetaApiController {

    @GetMapping("/status")
    public String status() {
        return "v2beta";
    }
}
