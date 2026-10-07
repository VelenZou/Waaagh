package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.waaagh.cloudfeignapi.User;
import org.waaagh.cloudfeignapi.UserApiConst;
import org.waaagh.feignclient.feign.config.UserConfiguration;

@FeignClient(path = UserApiConst.USER_CLIENT_BASE, value = "cloud-feign-server", contextId = "user", configuration = UserConfiguration.class, fallbackFactory = MyTestFallBackFactory.class)
public interface UserClient3 {

  @GetMapping(UserApiConst.USER_CLIENT_PARALLEL_SCAN9_ID)
  User parallelScan8(@PathVariable("id") Long id);

}