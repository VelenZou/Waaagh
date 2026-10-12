package org.waaagh.cloudfeignserver.service;

import org.springframework.stereotype.Service;
import org.waaagh.cloudfeignapi.User;

/**
 * Call Hierarchy 递归回归用例（服务端被调用链）。
 * <p>
 * {@code buildUser()} → {@code normalize(...)}：用于验证从 Feign 出发的 Callees 视图会在
 * Controller 之下继续进入服务端实现。详见 sample 根目录 CALL_HIERARCHY.md。
 */
@Service
public class HierarchyService {

    public User buildUser() {
        return normalize(new User(1L, "hierarchy"));
    }

    User normalize(User user) {
        return user;
    }
}
