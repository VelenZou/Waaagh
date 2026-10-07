package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.waaagh.cloudfeignapi.User;
import org.waaagh.feignclient.feign.config.UserConfiguration;

/**
 * 同路径 / 无目标形状用例（完整路径前缀 /hello/world/user）。
 * <p>
 * 预期匹配（详见 sample 根目录 METHOD_MATCHING.md）：
 * <ul>
 *   <li>samePathFirst、samePathSecond（GET /user/same-path/{id}）→ 两者都指向
 *       UserServerController#samePath；</li>
 *   <li>twinFirst、twinSecond（GET /user/twin/{id}）→ 两者都指向
 *       UserServerController#twinFirst；</li>
 *   <li>clientOnly（GET /user/client-only/{id}）→ ❌ 没有对应 Controller，
 *       不应出现导航图标。</li>
 * </ul>
 */
@FeignClient(path = "/hello/world/user", value = "cloud-feign-server", contextId = "user-same-paths",
        configuration = UserConfiguration.class)
public interface UserClientSamePaths {

    @GetMapping(value = "/same-path/{id}")
    User samePathFirst(@PathVariable("id") Long id);

    @GetMapping(value = "/same-path/{id}")
    User samePathSecond(@PathVariable("id") Long id);

    @GetMapping(value = "/twin/{id}")
    User twinFirst(@PathVariable("id") Long id);

    @GetMapping(value = "/twin/{id}")
    User twinSecond(@PathVariable("id") Long id);

    @GetMapping(value = "/client-only/{id}")
    User clientOnly(@PathVariable("id") Long id);
}
