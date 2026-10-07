package org.waaagh.feignclient.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.waaagh.cloudfeignapi.Order;
import org.waaagh.cloudfeignapi.Result;
import org.waaagh.feignclient.feign.client.OrderClient;

/**
 * 消费者 Controller（feign-order-client 模块自身暴露的接口），完整路径 /consumer/feign/order/get/{id}。
 * <p>
 * ❌ 项目中没有 Feign 客户端声明该路径（OrderClient 的路径是 /prefix/order/get/{id}），不参与匹配；
 * 这里仅演示“Controller 调用 Feign 客户端”（见 orderClient.getOrderById）。
 */
// curl http://localhost:9000/consumer/feign/order/get/1
@RestController
public class OrderClientController {

  @Resource
  private OrderClient orderClient;

  @GetMapping(value = "/consumer/feign/order/get/{id}")
  /**
   *
   */
  public Result<Order> getOrderById(@PathVariable("id") Long id) {
    Order order = orderClient.getOrderById(id);
    return new Result<>(order);
  }
}