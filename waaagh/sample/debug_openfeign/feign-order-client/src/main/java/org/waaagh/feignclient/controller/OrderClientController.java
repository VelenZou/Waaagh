package org.waaagh.feignclient.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.waaagh.cloudfeignapi.Order;
import org.waaagh.cloudfeignapi.Result;
import org.waaagh.feignclient.feign.client.OrderClientMissingContextPath;

/**
 * 消费者 Controller（本模块自身暴露的接口）：演示 Controller 调用 Feign 客户端。
 * <p>
 * 完整路径 /consumer/feign/order/get/{id} 没有对应的 Feign 声明，不参与匹配；
 * 调用的 {@link OrderClientMissingContextPath} 本身是缺前缀的负向对照。
 */
// curl http://localhost:9000/consumer/feign/order/get/1
@RestController
public class OrderClientController {

    @Resource
    private OrderClientMissingContextPath orderClient;

    @GetMapping(value = "/consumer/feign/order/get/{id}")
    public Result<Order> getOrderById(@PathVariable("id") Long id) {
        Order order = orderClient.getOrderById(id);
        return new Result<>(order);
    }
}
