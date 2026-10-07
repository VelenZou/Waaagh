package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.waaagh.cloudfeignapi.Order;
import org.waaagh.feignclient.feign.config.OrderConfiguration;

// http://localhost:9000/consumer/feign/order/get/1
@FeignClient(value = "cloud-feign-server", contextId = "order", configuration = OrderConfiguration.class)
public interface OrderClient {

  @GetMapping(value = "/prefix/order/get/{id}")
  Order getOrderById(@PathVariable("id") Long id);

  @GetMapping(value = "/prefix/order/get/{id}")
  Order getOrderById2(@PathVariable("id") Long id);

}