package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.waaagh.cloudfeignapi.User;
import org.waaagh.feignclient.feign.config.UserConfiguration;

/**
 * 第二个用户 Feign 客户端：只声明一个与 {@link UserClient#getUserById} 完全相同路径
 * （DELETE /hello/world/user/get/{id}）的方法。
 * <p>
 * 用于验证两个方向的多对多：
 * <ul>
 *   <li>正向：本方法 → 3 个 Controller 方法（与 UserClient#getUserById 相同）；</li>
 *   <li>Call Hierarchy：任一 Controller 方法的 Callers 应同时列出本方法与其他客户端方法。</li>
 * </ul>
 */
@FeignClient(path = "/hello/world/user", value = "cloud-feign-server", contextId = "user-delete",
        configuration = UserConfiguration.class)
public interface UserDeleteClient {

    @DeleteMapping(value = "/get/{id}")
    User getUserById(@PathVariable("id") Long id);
}
