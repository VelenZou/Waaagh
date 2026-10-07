package org.waaagh.cloudfeignserver.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.waaagh.cloudfeignapi.User;

/**
 * HTTP 请求方法匹配规则的验证用 Controller，与 MethodMatchClient 成对。
 * <p>
 * 匹配规则：完整路径相同，且 HTTP 请求方法兼容：
 * <ul>
 *     <li>双方都指定了方法：必须相同（多值时取集合交集）才匹配；</li>
 *     <li>任一方未指定方法（裸 {@code @RequestMapping}）：视为通配，匹配任意方法；</li>
 *     <li>多值 {@code method = {RequestMethod.GET, RequestMethod.POST}}：按集合交集判断。</li>
 * </ul>
 * 每个方法注释标注了预期结果（✅/❌）。完整对照表见 sample 根目录 METHOD_MATCHING.md。
 */
@RestController
@RequestMapping("/method")
public class MethodMatchServerController {

  /**
   * ✅ 应匹配 MethodMatchClient#sameVerbGet（同路径 + 同方法 GET）。
   * ❌ 不应匹配 MethodMatchClient#sameVerbDelete（同路径但方法不同）。
   */
  @GetMapping("/same-verb/{id}")
  public User sameVerbGet(@PathVariable("id") Long id) {
    return new User(id, "same-verb-get");
  }

  /**
   * ✅ 应匹配 MethodMatchClient#sameVerbDelete（同路径 + 同方法 DELETE）。
   * ❌ 不应匹配 MethodMatchClient#sameVerbGet（同路径但方法不同）。
   */
  @DeleteMapping("/same-verb/{id}")
  public User sameVerbDelete(@PathVariable("id") Long id) {
    return new User(id, "same-verb-delete");
  }

  /**
   * ✅ 应匹配 MethodMatchClient#wildcardController：
   * Controller 未指定方法 → 通配，GET 的客户端方法也能匹配。
   */
  @RequestMapping("/wildcard-controller")
  public User wildcardController() {
    return new User(0L, "wildcard-controller");
  }

  /**
   * ✅ 应匹配 MethodMatchClient#wildcardClient（客户端未指定方法 → 通配）。
   */
  @GetMapping("/wildcard-client")
  public User wildcardClient() {
    return new User(0L, "wildcard-client");
  }

  /**
   * ✅ 应匹配 MethodMatchClient#multiMethodPost（{GET, POST} 与 POST 有交集）。
   * ❌ 不应匹配 MethodMatchClient#multiMethodDelete（DELETE 不在集合内）。
   */
  @RequestMapping(value = "/multi-method", method = {RequestMethod.GET, RequestMethod.POST})
  public User multiMethod() {
    return new User(0L, "multi-method");
  }

  /**
   * ✅ 应匹配 MethodMatchClient#explicitGet：
   * {@code @RequestMapping(method = GET)} 与 {@code @GetMapping} 归一化后方法相同。
   */
  @RequestMapping(value = "/explicit-method", method = RequestMethod.GET)
  public User explicitGet() {
    return new User(0L, "explicit-get");
  }

  /**
   * ❌ 不应匹配 MethodMatchClient#postOnlyGet（POST vs GET，无交集）：
   * 客户端方法上不应出现导航图标。
   */
  @PostMapping("/post-only")
  public User postOnly() {
    return new User(0L, "post-only");
  }

}
