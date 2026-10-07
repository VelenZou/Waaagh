package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.waaagh.cloudfeignapi.User;

/**
 * 反例（负向对照）：故意不带服务端配置的 /v2 前缀。
 * <p>
 * 完整路径是 /hello/world/v2user/get/{id}，而 V2UserApiController 启用前缀后的完整路径是
 * /hello/world/v2/v2user/get/{id}，因此本客户端<b>不应匹配任何 Controller</b>：
 * ❌ 无跳转图标、Ctrl+Alt+B 结果为空。
 */
@FeignClient(path = "/hello/world/v2user", value = "cloud-feign-server", contextId = "v2user-noprefix")
public interface V2UserNoPrefixClient {

    @GetMapping("/get/{id}")
    User getById(@PathVariable("id") Long id);
}
