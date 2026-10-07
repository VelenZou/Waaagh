package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.waaagh.cloudfeignapi.User;

/**
 * 验证「按子包加前缀」：服务端 V2UserApiController 的完整路径包含配置注册的 /v2 前缀，
 * 客户端 @FeignClient#path 必须带上 /v2 才能匹配。
 * <p>
 * 预期匹配：
 * <ul>
 *   <li>getById（GET /hello/world/v2/v2user/get/{id}）→ V2UserApiController#getById；</li>
 *   <li>save（POST /hello/world/v2/v2user/save）→ V2UserApiController#save。</li>
 * </ul>
 */
@FeignClient(path = "/hello/world/v2/v2user", value = "cloud-feign-server", contextId = "v2user")
public interface V2UserClient {

    @GetMapping("/get/{id}")
    User getById(@PathVariable("id") Long id);

    @PostMapping("/save")
    User save(@RequestBody User user);
}
