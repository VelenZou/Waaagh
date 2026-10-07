package org.waaagh.cloudfeignserver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 继承场景的服务端控制器，端点与 {@code InheritanceApi}（由客户端的 {@code InheritanceFeignClient} 继承）一一对应：
 * server.servlet.context-path=/hello + spring.mvc.servlet.path=/world + /inheritance/... 。
 * <p>
 * 预期匹配：InheritanceApi#getById（GET /get/{id}）与 #save（POST /save），
 * 完整路径通过 InheritanceFeignClient 的 path=/hello/world/inheritance 计算。
 */
@RestController
public class InheritanceServerController {

    @GetMapping(value = "/inheritance/get/{id}")
    public String getById(@PathVariable("id") Long id) {
        return "inheritance-" + id;
    }

    @PostMapping(value = "/inheritance/save")
    public String save(@RequestBody Long value) {
        return "saved-" + value;
    }
}
