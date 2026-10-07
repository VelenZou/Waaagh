package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.waaagh.cloudfeignapi.Order;
import org.waaagh.feignclient.feign.config.OrderConfiguration;

/**
 * 缺前缀负向对照：本客户端没有设置 {@code path}，完整路径只有方法路径
 * /prefix/order/get/{id}；而 OrderServerController 的完整路径是
 * /hello/world/prefix/order/get/{id}（context-path=/hello + servlet path=/world + 类级 /prefix）。
 * <p>
 * ❌ 预期不匹配任何 Controller：不应出现导航图标，Ctrl+Alt+B 结果为空。
 * <p>
 * 方法本身也有重复映射（getOrderById / getOrderByIdAlias 同路径），与 OrderServerController
 * 的重复映射互为镜像。若要演示正向匹配，给 @FeignClient 补上 {@code path = "/hello/world"} 即可。
 */
@FeignClient(value = "cloud-feign-server", contextId = "order", configuration = OrderConfiguration.class)
public interface OrderClientMissingContextPath {

    @GetMapping(value = "/prefix/order/get/{id}")
    Order getOrderById(@PathVariable("id") Long id);

    @GetMapping(value = "/prefix/order/get/{id}")
    Order getOrderByIdAlias(@PathVariable("id") Long id);
}
