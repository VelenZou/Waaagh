package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.waaagh.cloudfeignapi.User;
import org.waaagh.feignclient.feign.config.UserConfiguration;

/**
 * 演示 Feign 客户端，完整路径前缀 /hello/world/user。
 * <p>
 * 预期匹配：deleUsr（DELETE /get/{id}）→ UserServerController/2/3 的 getUserById，
 * 以及 ListenerServerController#getUserById、NullServerController#delb（共 5 个 Controller）。
 */
@FeignClient(path = "/hello/world/user", value = "cloud-feign-server", contextId = "user", configuration = UserConfiguration.class)
public interface NullClient {

  @DeleteMapping(value = "/get/{id}")
  User deleUsr(@PathVariable("id") Long id);

}
