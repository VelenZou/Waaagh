package org.lyflexi.cloudfeignserver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运力服务端控制器，端点路径与 {@code TransportService}（继承自 {@code TransportServiceApi}）一一对应：
 * server.servlet.context-path=/hello + spring.mvc.servlet.path=/world + /transport/... 。
 */
@RestController
public class TransportServerController {

    @GetMapping(value = "/transport/get/{id}")
    public String getTransportById(@PathVariable("id") Long id) {
        return "transport-" + id;
    }

    @PostMapping(value = "/transport/vehicleEvent")
    public String vehicleEvent(@RequestBody Long orderId) {
        return "ok-" + orderId;
    }
}
