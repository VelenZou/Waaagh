package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.waaagh.cloudfeignapi.User;
import org.waaagh.feignclient.feign.config.UserConfiguration;

/**
 * 演示 Feign 客户端，完整路径前缀 /hello/world/user。
 * <p>
 * 预期匹配（插件按“路径 + HTTP 方法”匹配，详见 sample 根目录 METHOD_MATCHING.md；下文
 * UserServerController/2/3 表示 UserServerController、UserServerController2、UserServerController3）：
 * <ul>
 *   <li>getUserById（DELETE /get/{id}）→ UserServerController/2/3 的 getUserById，
 *       以及 ListenerServerController#getUserById、NullServerController#delb；</li>
 *   <li>update、del、getfather（GET）→ UserServerController/2/3 的同名方法。</li>
 * </ul>
 */
// http://localhost:9000/consumer/feign/user/get/1
@FeignClient(path = "/hello/world/user", value = "cloud-feign-server", contextId = "user", configuration = UserConfiguration.class)
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
