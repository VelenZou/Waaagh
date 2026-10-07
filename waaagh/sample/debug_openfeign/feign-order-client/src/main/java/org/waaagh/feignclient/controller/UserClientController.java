package org.waaagh.feignclient.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.waaagh.cloudfeignapi.Result;
import org.waaagh.cloudfeignapi.User;
import org.waaagh.feignclient.feign.client.UserClient;

//curl http://localhost:9000/consumer/feign/user/get/1
@RestController
public class UserClientController {

  @Autowired
  private UserClient userClient;

  @GetMapping(value = "/consumer/feign/user/get/{id}")
  public Result<User> getUserById(@PathVariable("id") Long id) {
    User user = userClient.getUserById(id);
    return new Result<>(user);
  }
}