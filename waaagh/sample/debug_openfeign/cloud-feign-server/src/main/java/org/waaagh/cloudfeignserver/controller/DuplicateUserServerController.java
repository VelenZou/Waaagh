package org.waaagh.cloudfeignserver.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.waaagh.cloudfeignapi.User;

/**
 * 与 {@link UserServerController} 故意重复的映射（一方法多目标用例）。
 * <p>
 * 预期匹配：UserClient#getUserById、UserDeleteClient#getUserById（DELETE /user/get/{id}）
 * 与 UserClient#update（GET /user/update2/{id}）应同时跳转本类与 UserServerController 的方法；
 * Call Hierarchy 的 Callees / Callers 视角下也应并列出现。
 */
@RestController
public class DuplicateUserServerController {

    @DeleteMapping(value = "/user/get/{id}")
    public User getUserById(@PathVariable("id") Long id) {
        return new User(id, "user");
    }

    @GetMapping(value = "/user/update2/{id}")
    public User update(@PathVariable("id") Long id) {
        return new User(id, "user");
    }
}
