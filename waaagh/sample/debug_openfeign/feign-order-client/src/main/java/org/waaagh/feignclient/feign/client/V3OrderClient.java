package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.waaagh.cloudfeignapi.Order;

/**
 * 验证 HandlerTypePredicate 注册的 /v3 前缀。
 * <p>
 * 预期匹配：detail（GET /hello/world/v3/v3order/detail/{id}）→ V3OrderApiController#detail。
 */
@FeignClient(path = "/hello/world/v3/v3order", value = "cloud-feign-server", contextId = "v3order")
public interface V3OrderClient {

    @GetMapping("/detail/{id}")
    Order detail(@PathVariable("id") Long id);
}
