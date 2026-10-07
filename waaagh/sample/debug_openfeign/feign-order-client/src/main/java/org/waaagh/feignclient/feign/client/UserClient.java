package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.waaagh.cloudfeignapi.User;
import org.waaagh.feignclient.feign.config.UserConfiguration;

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
