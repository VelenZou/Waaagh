package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.waaagh.cloudfeignapi.User;
import org.waaagh.feignclient.feign.config.UserConfiguration;

@FeignClient(path = "/hello/world/user", value = "cloud-feign-server", contextId = "user", configuration = UserConfiguration.class)
public interface NullClient {

  @DeleteMapping(value = "/get/{id}")
  User deleUsr(@PathVariable("id") Long id);

}
