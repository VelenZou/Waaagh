package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 验证前缀规则的 startsWith 语义：服务端包名 controller.v2beta 也会命中规则一的 /v2 前缀。
 * <p>
 * 预期匹配：status（GET /hello/world/v2/v2beta-api/status）→ V2BetaApiController#status。
 */
@FeignClient(path = "/hello/world/v2/v2beta-api", value = "cloud-feign-server", contextId = "v2beta")
public interface V2BetaClient {

    @GetMapping("/status")
    String status();
}
