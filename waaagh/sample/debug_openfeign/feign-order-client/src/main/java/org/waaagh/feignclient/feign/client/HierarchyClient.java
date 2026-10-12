package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.waaagh.cloudfeignapi.User;

/**
 * Call Hierarchy 递归回归用例（客户端侧）。
 * <p>
 * 完整路径 = @FeignClient path(/hello/world/hierarchy) + 方法路径 /tree
 * → 匹配 {@code HierarchyServerController#tree}（GET）。
 * <p>
 * 本方法被 {@code HierarchyCaller#invoke()} 调用，后者又被 {@code HierarchyCaller#run()} 调用。
 * 因此在 {@code HierarchyServerController#tree} 的 Callers 视图里应能一路向上展开：
 * Controller ← Feign ← HierarchyCaller#invoke ← HierarchyCaller#run。
 * 详见 sample 根目录 CALL_HIERARCHY.md。
 */
@FeignClient(path = "/hello/world/hierarchy", value = "cloud-feign-server", contextId = "hierarchy")
public interface HierarchyClient {

    @GetMapping("/tree")
    User tree();
}
