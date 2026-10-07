package org.waaagh.feignclient.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.waaagh.cloudfeignapi.User;

/**
 * HTTP 请求方法匹配规则的验证用 Feign 客户端，与 MethodMatchServerController 成对。
 * <p>
 * 匹配规则：完整路径相同 + HTTP 请求方法兼容；任一方未指定方法视为通配。
 * 每个方法注释标注了预期结果（✅/❌）。完整对照表见 sample 根目录 METHOD_MATCHING.md。
 */
@FeignClient(path = "/hello/world/method", value = "cloud-feign-server", contextId = "methodMatch")
public interface MethodMatchClient {

  /**
   * ✅ 唯一目标：MethodMatchServerController#sameVerbGet。
   * ❌ 结果中不应出现 MethodMatchServerController#sameVerbDelete（方法不同）。
   */
  @GetMapping("/same-verb/{id}")
  User sameVerbGet(@PathVariable("id") Long id);

  /**
   * ✅ 唯一目标：MethodMatchServerController#sameVerbDelete。
   * ❌ 结果中不应出现 MethodMatchServerController#sameVerbGet（方法不同）。
   */
  @DeleteMapping("/same-verb/{id}")
  User sameVerbDelete(@PathVariable("id") Long id);

  /**
   * ✅ 应匹配 MethodMatchServerController#wildcardController（Controller 未指定方法 → 通配）。
   */
  @GetMapping("/wildcard-controller")
  User wildcardController();

  /**
   * ✅ 应匹配 MethodMatchServerController#wildcardClient（客户端未指定方法 → 通配）。
   * 注意：这里刻意不写 method 属性，用于验证“未指定 = 通配”的匹配规则。
   */
  @RequestMapping("/wildcard-client")
  User wildcardClient();

  /**
   * ✅ 应匹配 MethodMatchServerController#multiMethod（POST 与 {GET, POST} 有交集）。
   */
  @PostMapping("/multi-method")
  User multiMethodPost();

  /**
   * ❌ 不应匹配 MethodMatchServerController#multiMethod（DELETE 与 {GET, POST} 无交集）：
   * 该方法不应出现导航图标，Go to Implementation 结果为空。
   */
  @DeleteMapping("/multi-method")
  User multiMethodDelete();

  /**
   * ✅ 应匹配 MethodMatchServerController#explicitGet（GET 归一化后相同）。
   */
  @GetMapping("/explicit-method")
  User explicitGet();

  /**
   * ❌ 不应匹配 MethodMatchServerController#postOnly（GET vs POST）：
   * 该方法不应出现导航图标，Go to Implementation 结果为空。
   */
  @GetMapping("/post-only")
  User postOnlyGet();

}
