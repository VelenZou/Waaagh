package org.waaagh.cloudfeignserver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.waaagh.cloudfeignapi.Order;

/**
 * 演示 Controller，完整路径前缀 /hello/world/prefix（context-path + servlet path + 类级 @RequestMapping）。
 * <p>
 * ⚠ 当前与 OrderClient/Order2Client 不匹配：客户端完整路径是 /prefix/order/get/{id}（未设置 @FeignClient.path），
 * 缺少 /hello/world 前缀。另外本类两个方法的映射完全相同，用于验证重复映射场景。
 */
@RestController
@RequestMapping("/prefix")
public class Order2ServerController {

  @GetMapping(value = "/order/get/{id}")
  public Order getPaymentById(@PathVariable("id") Long id) {
    return new Order(id, "order");
  }

  @GetMapping(value = "/order/get/{id}")
  public Order getPaymentById2(@PathVariable("id") Long id) {
    return new Order(id, "order");
  }
}