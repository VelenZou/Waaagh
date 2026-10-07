package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.waaagh.cloudfeignapi.User;
import org.waaagh.feignclient.feign.config.UserConfiguration;

/**
 * 用户 Feign 客户端（基础演示对，完整路径前缀 /hello/world/user）。
 * <p>
 * 预期匹配（匹配规则：完整路径 + HTTP 方法，详见 sample 根目录 METHOD_MATCHING.md）：
 * <ul>
 *   <li>getUserById（DELETE /user/get/{id}）→ 3 个 Controller 方法（一方法多目标用例）：
 *       UserServerController#getUserById、DuplicateUserServerController#getUserById、
 *       UserDeleteAliasController#deleteUser；</li>
 *   <li>update（GET /user/update2/{id}，方法名与路径不一致）→
 *       UserServerController#update、DuplicateUserServerController#update；</li>
 *   <li>del、getfather（GET）→ UserServerController 的同名方法。</li>
 * </ul>
 */
@FeignClient(path = "/hello/world/user", value = "cloud-feign-server", contextId = "user",
        configuration = UserConfiguration.class)
public interface UserClient {

    @DeleteMapping(value = "/get/{id}")
    User getUserById(@PathVariable("id") Long id);

    @GetMapping(value = "/update2/{id}")
    User update(@PathVariable("id") Long id);

    @GetMapping(value = "/del/{id}")
    User del(@PathVariable("id") Long id);

    @GetMapping(value = "/getfather/{id}")
    User getfather(@PathVariable("id") Long id);
}
