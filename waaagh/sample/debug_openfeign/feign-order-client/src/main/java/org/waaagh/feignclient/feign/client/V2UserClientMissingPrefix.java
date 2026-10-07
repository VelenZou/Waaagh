package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.waaagh.cloudfeignapi.User;

/**
 * 前缀负向对照：故意不带服务端配置注册的 /v2 前缀。
 * <p>
 * 本客户端完整路径是 /hello/world/v2user/get/{id}（缺少 /v2），
 * 而 V2UserApiController 启用前缀后是 /hello/world/v2/v2user/get/{id}，
 * 因此不应匹配任何 Controller：❌ 无跳转图标、Ctrl+Alt+B 结果为空。
 */
@FeignClient(path = "/hello/world/v2user", value = "cloud-feign-server", contextId = "v2user-missing-prefix")
public interface V2UserClientMissingPrefix {

    @GetMapping("/get/{id}")
    User getById(@PathVariable("id") Long id);
}
