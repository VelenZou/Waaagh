package org.waaagh.cloudfeignserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.waaagh.cloudfeignapi.User;
import org.waaagh.cloudfeignserver.service.HierarchyService;

/**
 * Call Hierarchy 递归回归用例（服务端侧）。
 * <p>
 * 完整路径 = server.servlet.context-path(/hello) + spring.mvc.servlet.path(/world)
 * + /hierarchy/tree，匹配 {@code HierarchyClient#tree}（GET）。
 * <p>
 * 方法体调用 {@code HierarchyService#buildUser()}（其内部再调用 {@code #normalize}）：
 * 因此从 {@code HierarchyClient#tree} 出发的 Callees 视图应能一路向下展开：
 * Feign → 本方法 → HierarchyService#buildUser → HierarchyService#normalize。
 * 详见 sample 根目录 CALL_HIERARCHY.md。
 */
@RestController
public class HierarchyServerController {

    @Autowired
    private HierarchyService hierarchyService;

    @GetMapping("/hierarchy/tree")
    public User tree() {
        return hierarchyService.buildUser();
    }
}
