package org.waaagh.cloudfeignserver.controller;

import org.waaagh.cloudfeignapi.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 演示 Controller：完整路径 /hello/world/user/get/{id}（DELETE），与 ListenerServerController 同路径同方法。
 * <p>
 * 预期匹配 7 个 Feign 方法：UserClient/2/4/5/6#getUserById + ListenerClient#delUsr、NullClient#deleUsr。
 */
@RestController
public class NullServerController {

  @DeleteMapping(value = "/user/get/{id}")
  public User delb(@PathVariable("id") Long id) {
    return new User(id, "user");
  }

}
