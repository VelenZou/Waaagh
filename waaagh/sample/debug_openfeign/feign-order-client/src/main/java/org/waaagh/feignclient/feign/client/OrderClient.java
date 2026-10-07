package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.waaagh.cloudfeignapi.Order;
import org.waaagh.feignclient.feign.config.OrderConfiguration;

/**
 * 演示 Feign 客户端（未设置 {@code path}，完整路径 = 方法路径 /prefix/order/get/{id}）。
 * <p>
 * ⚠ 与 OrderServerController/Order2ServerController 不匹配：服务端的完整路径是
 * /hello/world/prefix/order/get/{id}（context-path=/hello + servlet path=/world + 类级 /prefix），
 * 客户端缺少 /hello/world 前缀。若要演示匹配，需要给 @FeignClient 补 {@code path = "/hello/world"}。
 */
// http://localhost:9000/consumer/feign/order/get/1
@FeignClient(value = "cloud-feign-server", contextId = "order", configuration = OrderConfiguration.class)
public interface OrderClient {

  @GetMapping(value = "/prefix/order/get/{id}")
  Order getOrderById(@PathVariable("id") Long id);

  @GetMapping(value = "/prefix/order/get/{id}")
  Order getOrderById2(@PathVariable("id") Long id);

}