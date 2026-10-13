package org.waaagh.feignclient.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.waaagh.cloudfeignapi.Result;
import org.waaagh.cloudfeignapi.User;
import org.waaagh.feignclient.feign.client.UserClient;

/**
 * 消费者 Controller（feign-order-client 模块自身暴露的接口），完整路径 /consumer/feign/user/get/{id}。
 * <p>
 * ❌ 项目中没有 Feign 客户端声明该路径，不参与 Feign ↔ Controller 匹配；
 * 这里仅演示“Controller 调用 Feign 客户端”（见 userClient.getUserById）。
 * <p>
 * 同时是 Ctrl+B 回归用例：在 {@code UserClient#getUserById} 声明上按 Ctrl+B，会弹出 IDEA 原生的
 * "Go to Declaration or Usages" usages 弹窗（顶部方法签名、中间 “Merge Usages from the Same Line”
 * 等筛选项、底部选中项文件路径），列表里同时列出 3 个匹配 Controller <b>和</b>本文件里
 * {@code userClient.getUserById(id)} 这个调用处——映射端点作为额外 usage 注入原生弹窗，
 * 插件不再自建 chooser。
 * <p>
 * 另一个回归点：列出的“调用者”必须能跳到调用处。凡是 javadoc 里的 {@code @link ...} 引用
 * （例如 {@code UserDeleteClient} 类注释里的 {@code @link UserClient#getUserById}）都不应作为
 * 调用者条目出现——那是“链接”而非“调用”，点进去只会跳回被链接的方法本身（Feign 定义处）。
 */
//curl http://localhost:9000/consumer/feign/user/get/1
@RestController
public class UserClientController {

  @Autowired
  private UserClient userClient;

  @GetMapping(value = "/consumer/feign/user/get/{id}")
  public Result<User> getUserById(@PathVariable("id") Long id) {
    User user = userClient.getUserById(id);
    return new Result<>(user);
  }
}