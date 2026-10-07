package org.waaagh.cloudfeignserver.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.waaagh.cloudfeignapi.User;

/**
 * DELETE 目标别名：完整路径 /hello/world/user/get/{id}（DELETE），与
 * UserServerController#getUserById、DuplicateUserServerController#getUserById 相同，
 * 但类名和方法名都与客户端不同。
 * <p>
 * 用于验证匹配只看「路径 + HTTP 方法」，与类名 / 方法名无关：
 * UserClient#getUserById、UserDeleteClient#getUserById 都应能跳到这里。
 */
@RestController
public class UserDeleteAliasController {

    @DeleteMapping(value = "/user/get/{id}")
    public User deleteUser(@PathVariable("id") Long id) {
        return new User(id, "user");
    }
}
