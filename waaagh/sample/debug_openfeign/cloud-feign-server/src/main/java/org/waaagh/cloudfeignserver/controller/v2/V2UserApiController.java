package org.waaagh.cloudfeignserver.controller.v2;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.waaagh.cloudfeignapi.User;

/**
 * 验证「按子包加前缀」：本类在 controller.v2 包下，{@code WaaaghPathPrefixConfig} 规则一会加前缀 /v2。
 * <p>
 * 完整路径 = /hello（context-path） + /world（servlet path） + /v2（前缀） + /v2user（类路径） + 方法路径：
 * <ul>
 *   <li>GET /hello/world/v2/v2user/get/{id} → 匹配 V2UserClient#getById；</li>
 *   <li>POST /hello/world/v2/v2user/save → 匹配 V2UserClient#save。</li>
 * </ul>
 * ❌ V2UserNoPrefixClient 少了 /v2，不匹配本类。
 */
@RestController
@RequestMapping("/v2user")
public class V2UserApiController {

    @GetMapping("/get/{id}")
    public User getById(@PathVariable("id") Long id) {
        return new User(id, "v2-user");
    }

    @PostMapping("/save")
    public User save(@RequestBody User user) {
        return user;
    }
}
