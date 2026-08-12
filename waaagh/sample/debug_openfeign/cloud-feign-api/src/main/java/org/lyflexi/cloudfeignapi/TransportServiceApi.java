package org.lyflexi.cloudfeignapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 基础 API 接口（不带 @FeignClient），承载 @RequestMapping 端点方法。
 * 被 {@code TransportService}（@FeignClient）继承，用于验证“Feign 继承父接口”场景的跳转/层级能力。
 */
public interface TransportServiceApi {

    @GetMapping(value = "/get/{id}")
    String getTransportById(@PathVariable("id") Long id);

    @PostMapping(value = "/vehicleEvent")
    String vehicleEvent(@RequestBody Long orderId);
}
