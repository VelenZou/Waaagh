package org.waaagh.feignclient.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.waaagh.cloudfeignapi.User;
import org.waaagh.feignclient.feign.client.HierarchyClient;

/**
 * Call Hierarchy 递归回归用例（调用方链路）。
 * <p>
 * {@code run()} → {@code invoke()} → {@link HierarchyClient#tree()}：用于验证从 Controller 出发的
 * Callers 视图会越过 Feign 继续向上找调用者（而不是停在 Feign）。
 * 详见 sample 根目录 CALL_HIERARCHY.md。
 */
@Service
public class HierarchyCaller {

    @Autowired
    private HierarchyClient hierarchyClient;

    public User run() {
        return invoke();
    }

    public User invoke() {
        return hierarchyClient.tree();
    }
}
