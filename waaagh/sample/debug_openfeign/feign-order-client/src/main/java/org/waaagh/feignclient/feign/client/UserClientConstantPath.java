package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.waaagh.cloudfeignapi.User;
import org.waaagh.cloudfeignapi.UserApiPaths;
import org.waaagh.feignclient.feign.config.UserConfiguration;

/**
 * 路径常量用例：{@code @FeignClient#path} 与方法路径都引用 {@link UserApiPaths} 中的常量。
 * <p>
 * 完整路径 = UserApiPaths.CLIENT_BASE(/hello/world) + UserApiPaths.CONSTANT_PATH(/user/const/{id})。
 * <p>
 * 预期匹配：constantPath（GET /hello/world/user/const/{id}）→ UserServerController#constantPath；
 * 用于验证常量引用（而非字面量）也能被正确解析并拼接。
 */
@FeignClient(path = UserApiPaths.CLIENT_BASE, value = "cloud-feign-server", contextId = "user-constant",
        configuration = UserConfiguration.class, fallbackFactory = ConstantPathFallbackFactory.class)
public interface UserClientConstantPath {

    @GetMapping(UserApiPaths.CONSTANT_PATH)
    User constantPath(@PathVariable("id") Long id);
}
