# 按子包路径前缀（addPathPrefix）匹配验证

Spring MVC 支持按 Controller 所在的包统一加路径前缀（常见于接口版本化）：

```java
@Configuration
public class WaaaghPathPrefixConfig implements WebMvcConfigurer {
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/v2",
                c -> c.getPackageName().startsWith("com.example.controller.v2"));
    }
}
```

Spring 规则：**按注册顺序取第一个命中的前缀**（`RequestMappingHandlerMapping#getPathPrefix`），
前缀拼在类路径之前：

```
server.servlet.context-path + spring.mvc.servlet.path + 前缀 + 类 @RequestMapping + 方法路径
```

插件会解析项目源码里的 `PathMatchConfigurer#addPathPrefix` 调用，把前缀拼进 Controller 完整路径；
因此 Feign ↔ Controller 匹配和 Copy Controller URL 都会包含前缀。

支持的 predicate 写法：

- `c -> c.getPackageName().startsWith("...")`（也支持 `equals` / `contains` / `endsWith` / `matches`；`&&` / `||` / `!` 可组合）
- `c -> c.isAnnotationPresent(Xxx.class)`
- `HandlerTypePredicate.forBasePackage(...)` / `forBasePackageClass(...)` / `forAnnotation(...)` / `forAssignableType(...)`
- 以上谓词的 `.and(...)` / `.or(...)` / `.negate()` 组合
- 前缀 / 包名的常量、静态字段、局部变量引用；`@Value("${key}")` 会按模块配置文件解析

解析不了的写法会跳过该条规则（宁可不加前缀，也不会加错前缀）。

## 用例

- context-path=`/hello`，servlet path=`/world`
- 配置类：`cloud-feign-server` → `org.waaagh.cloudfeignserver.config.WaaaghPathPrefixConfig`
  - 规则一：`controller.v2*` 包（lambda：`@RestController` + `getPackageName().startsWith`）→ `/v2`
  - 规则二：`controller.v3` 包（`HandlerTypePredicate.forBasePackage`，前缀用常量）→ `/v3`

| # | Client 方法 | 客户端完整路径 | 对应 Controller 方法 | 预期结果 |
|---|---|---|---|---|
| 1 | `V2UserClient#getById`（GET） | `/hello/world/v2/v2user/get/{id}` | `V2UserApiController#getById` | ✅ 图标 + Ctrl+Alt+B 命中 |
| 2 | `V2UserClient#save`（POST） | `/hello/world/v2/v2user/save` | `V2UserApiController#save` | ✅（前缀 + 方法都匹配） |
| 3 | `V2UserClientMissingPrefix#getById`（GET） | `/hello/world/v2user/get/{id}`（缺 `/v2`） | — | ❌ 无图标，Ctrl+Alt+B 为空 |
| 4 | `V3OrderClient#detail`（GET） | `/hello/world/v3/v3order/detail/{id}` | `V3OrderApiController#detail` | ✅（HandlerTypePredicate 前缀 + 常量） |
| 5 | `V2BetaClient#status`（GET） | `/hello/world/v2/v2beta-api/status` | `V2BetaApiController#status` | ✅（`controller.v2beta` 命中 `startsWith`，与 Spring 一致） |
| 6 | 既有 `UserClient#update`（GET） | `/hello/world/user/update2/{id}` | `UserServerController#update` | ✅ 回归：未命中任何规则的包不加前缀 |

Copy URL 预期（Controller 侧）：

| Controller 方法 | Copy Controller URL 结果 |
|---|---|
| `V2UserApiController#getById` | `/hello/world/v2/v2user/get/{id}` |
| `V3OrderApiController#detail` | `/hello/world/v3/v3order/detail/{id}` |
| `V2BetaApiController#status` | `/hello/world/v2/v2beta-api/status` |

## 怎么测

1. `cd waaagh && ./gradlew runIde`
2. 等示例工程的 Maven import 完成（见根目录 AGENTS.md 的说明），打开各 Client：
   - ✅ 行：方法上应出现导航图标，点击落到对应的 Controller 方法；
   - ❌ 行：不应出现图标，`Ctrl+Alt+B`（Go to Implementation）结果为空。
3. 反向验证：打开对应 Controller，用 gutter 图标 / `Ctrl+Alt+B` / `Ctrl+Alt+H`（Call Hierarchy）检查匹配到的 Client，与表格互为镜像。
4. 复制 URL：点击 Controller 方法注解行的 "Copy Controller URL" 图标，剪贴板内容应为表中的完整路径（含前缀）。
5. 注意：全量扫描结果按项目缓存；保存文件、或外部编辑器（Cursor 等）的修改被 IDEA 同步后，
   缓存会自动失效，下次查询自动重扫，**无需重启 / 重开项目**；单个方法的 Copy URL 本就是即时重算的。
