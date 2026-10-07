package org.waaagh.cloudfeignserver.controller.v3;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.waaagh.cloudfeignapi.Order;

/**
 * 验证 HandlerTypePredicate.forBasePackage 注册的前缀：本类在 controller.v3 包下，前缀 /v3。
 * <p>
 * 完整路径 GET /hello/world/v3/v3order/detail/{id} → 匹配 V3OrderClient#detail。
 */
@RestController
@RequestMapping("/v3order")
public class V3OrderApiController {

    @GetMapping("/detail/{id}")
    public Order detail(@PathVariable("id") Long id) {
        return new Order(id, "v3-order");
    }
}
