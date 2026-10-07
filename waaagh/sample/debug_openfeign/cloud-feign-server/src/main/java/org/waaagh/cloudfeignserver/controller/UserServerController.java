package org.waaagh.cloudfeignserver.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.waaagh.cloudfeignapi.User;
import org.waaagh.cloudfeignapi.UserApiPaths;

/**
 * 用户服务端 Controller（基础目标）。
 * <p>
 * 完整路径 = server.servlet.context-path(/hello) + spring.mvc.servlet.path(/world) + 方法路径。
 * <p>
 * 预期匹配的 Feign 方法（匹配规则：完整路径 + HTTP 方法）：
 * <ul>
 *   <li>getUserById（DELETE /user/get/{id}）→ UserClient#getUserById、UserDeleteClient#getUserById；</li>
 *   <li>update（GET /user/update2/{id}）→ UserClient#update；</li>
 *   <li>del、getfather（GET）→ UserClient 的同名方法；</li>
 *   <li>samePath（GET /user/same-path/{id}）→ UserClientSamePaths#samePathFirst / #samePathSecond（同路径多方法）；</li>
 *   <li>twinFirst（GET /user/twin/{id}）→ UserClientSamePaths#twinFirst / #twinSecond；</li>
 *   <li>constantPath（路径来自 UserApiPaths.CONSTANT_PATH）→ UserClientConstantPath#constantPath。</li>
 * </ul>
 */
@RestController
public class UserServerController {

    @DeleteMapping(value = "/user/get/{id}")
    public User getUserById(@PathVariable("id") Long id) {
        return new User(id, "user");
    }

    @GetMapping(value = "/user/update2/{id}")
    public User update(@PathVariable("id") Long id) {
        return new User(id, "user");
    }

    @GetMapping(value = "/user/del/{id}")
    public User del(@PathVariable("id") Long id) {
        return new User(id, "user");
    }

    @GetMapping(value = "/user/getfather/{id}")
    public User getfather(@PathVariable("id") Long id) {
        return new User(id, "user");
    }

    @GetMapping(value = "/user/same-path/{id}")
    public User samePath(@PathVariable("id") Long id) {
        return new User(id, "user");
    }

    @GetMapping(value = "/user/twin/{id}")
    public User twinFirst(@PathVariable("id") Long id) {
        return new User(id, "user");
    }

    @GetMapping(UserApiPaths.CONSTANT_PATH)
    public User constantPath(@PathVariable("id") Long id) {
        return new User(id, "user");
    }
}
