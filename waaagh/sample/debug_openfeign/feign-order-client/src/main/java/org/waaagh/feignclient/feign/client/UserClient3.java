package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.waaagh.cloudfeignapi.User;
import org.waaagh.cloudfeignapi.UserApiConst;
import org.waaagh.feignclient.feign.config.UserConfiguration;

/**
 * 演示 Feign 客户端，完整路径前缀来自常量 UserApiConst.USER_CLIENT_BASE（"/hello/world"）。
 * <p>
 * 预期匹配：parallelScan8（实际路径是 {@code UserApiConst.USER_CLIENT_PARALLEL_SCAN9_ID}
 * = /user/parallelScan9/{id}，GET）→ UserServerController/2/3 的 parallelScan9。
 * 方法名与路径并非同名，用于验证常量路径解析。
 */
@FeignClient(path = UserApiConst.USER_CLIENT_BASE, value = "cloud-feign-server", contextId = "user", configuration = UserConfiguration.class, fallbackFactory = MyTestFallBackFactory.class)
public interface UserClient3 {

  @GetMapping(UserApiConst.USER_CLIENT_PARALLEL_SCAN9_ID)
  User parallelScan8(@PathVariable("id") Long id);

}