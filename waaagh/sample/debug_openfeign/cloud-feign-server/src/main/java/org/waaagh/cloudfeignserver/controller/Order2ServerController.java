package org.waaagh.cloudfeignserver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.waaagh.cloudfeignapi.Order;

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