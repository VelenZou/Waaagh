package org.waaagh.cloudfeignserver.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.waaagh.cloudfeignapi.User;
import org.waaagh.cloudfeignserver.service.UserService;

/**
 * 与 {@link UserServerController} 故意重复的映射（一方法多目标用例）。
 * <p>
 * 预期匹配：UserClient#getUserById、UserDeleteClient#getUserById（DELETE /user/get/{id}）
 * 与 UserClient#update（GET /user/update2/{id}）应同时跳转本类与 UserServerController 的方法；
 * Call Hierarchy 的 Callees / Callers 视角下也应并列出现。
 * <p>
 * 同时是 Ctrl+B 回归用例：方法体内对 {@code userService} / {@code getUserById()} 的引用
 * 必须走 IDEA 原生跳转（字段声明 / UserService 方法），而不是跳到 Feign。
 */
@RestController
@RequiredArgsConstructor
public class DuplicateUserServerController {

    private final UserService userService;

    @DeleteMapping(value = "/user/get/{id}")
    public User getUserById(@PathVariable("id") Long id) {
        return userService.getUserById();
    }

    @GetMapping(value = "/user/update2/{id}")
    public User update(@PathVariable("id") Long id) {
        return new User(id, "user");
    }
}
