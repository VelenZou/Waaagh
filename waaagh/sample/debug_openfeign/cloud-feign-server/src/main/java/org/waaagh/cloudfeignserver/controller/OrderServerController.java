package org.waaagh.cloudfeignserver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.waaagh.cloudfeignapi.Order;

/**
 * 重复映射形状用例：类级 @RequestMapping("/prefix")，两个方法映射完全相同
 * （GET /hello/world/prefix/order/get/{id}）。
 * <p>
 * 与 OrderClientMissingContextPath 构成负向对照：客户端没写 @FeignClient#path，
 * 完整路径只有 /prefix/order/get/{id}，缺少 /hello/world，因此双方不匹配。
 */
@RestController
@RequestMapping("/prefix")
public class OrderServerController {

    @GetMapping(value = "/order/get/{id}")
    public Order getOrderById(@PathVariable("id") Long id) {
        return new Order(id, "order");
    }

    @GetMapping(value = "/order/get/{id}")
    public Order getOrderByIdAlias(@PathVariable("id") Long id) {
        return new Order(id, "order");
    }
}
