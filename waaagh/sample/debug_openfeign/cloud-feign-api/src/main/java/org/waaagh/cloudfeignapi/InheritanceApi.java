package org.waaagh.cloudfeignapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 继承场景的父接口（不带 @FeignClient），只声明端点方法。
 * <p>
 * 客户端的 {@code InheritanceFeignClient}（@FeignClient 子接口）继承本接口，
 * 用于验证「端点在父接口、@FeignClient 在子接口」的继承场景：
 * 跳转与 Call Hierarchy 应把父接口上的方法也识别为 Feign 方法。
 * <p>
 * 预期匹配（完整路径 = 客户端 path(/hello/world/inheritance) + 方法路径）：
 * <ul>
 *   <li>getById（GET /get/{id}）→ InheritanceServerController#getById</li>
 *   <li>save（POST /save）→ InheritanceServerController#save</li>
 * </ul>
 */
public interface InheritanceApi {

    @GetMapping(value = "/get/{id}")
    String getById(@PathVariable("id") Long id);

    @PostMapping(value = "/save")
    String save(@RequestBody Long value);
}
