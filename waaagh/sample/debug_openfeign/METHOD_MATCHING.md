# HTTP 请求方法匹配规则验证

插件匹配规则：**完整路径相同 + HTTP 请求方法兼容**。

- 双方都指定了方法 → 必须相同（多值取集合交集）才匹配；
- 任一方未指定方法（裸 `@RequestMapping`）→ 视为通配，匹配任意方法；
- `method = {RequestMethod.GET, RequestMethod.POST}` → 按集合交集判断。

验证对（完整路径前缀均为 `/hello/world/method`）：

- Client：`feign-order-client` → `org.waaagh.feignclient.feign.client.MethodMatchClient`
- Controller：`cloud-feign-server` → `org.waaagh.cloudfeignserver.controller.MethodMatchServerController`

## 对照表

| # | Client 方法 | Client 注解 | Controller 方法 | Controller 注解 | 预期结果 |
|---|---|---|---|---|---|
| 1 | `sameVerbGet` | `@GetMapping("/same-verb/{id}")` | `sameVerbGet` | `@GetMapping` | ✅ 唯一目标 |
| 2 | `sameVerbGet` | `@GetMapping` | `sameVerbDelete` | `@DeleteMapping` | ❌ 不出现 |
| 3 | `sameVerbDelete` | `@DeleteMapping("/same-verb/{id}")` | `sameVerbDelete` | `@DeleteMapping` | ✅ 唯一目标 |
| 4 | `sameVerbDelete` | `@DeleteMapping` | `sameVerbGet` | `@GetMapping` | ❌ 不出现 |
| 5 | `wildcardController` | `@GetMapping("/wildcard-controller")` | `wildcardController` | `@RequestMapping`（未指定方法） | ✅ 通配匹配 |
| 6 | `wildcardClient` | `@RequestMapping("/wildcard-client")`（未指定方法） | `wildcardClient` | `@GetMapping` | ✅ 通配匹配 |
| 7 | `multiMethodPost` | `@PostMapping("/multi-method")` | `multiMethod` | `@RequestMapping(method = {GET, POST})` | ✅ 交集匹配 |
| 8 | `multiMethodDelete` | `@DeleteMapping("/multi-method")` | `multiMethod` | `@RequestMapping(method = {GET, POST})` | ❌ 无交集，无图标 |
| 9 | `explicitGet` | `@GetMapping("/explicit-method")` | `explicitGet` | `@RequestMapping(method = GET)` | ✅ 归一化后相同 |
| 10 | `postOnlyGet` | `@GetMapping("/post-only")` | `postOnly` | `@PostMapping` | ❌ 无图标 |

> 修复前的行为对照：#2 / #4 会各自跳到两个方法（只比 path），#8 / #10 也会错误地出现图标。

## 既有 fixtures 速查

既有示例的逐类匹配关系写在各类的 Javadoc 注释里（打开类文件即可对照），要点：

- `/hello/world/user/**`（GET/DELETE 同名匹配）：
  - `UserClient`、`UserClient2`、`UserClient4`、`UserClient5`、`UserClient6` ↔ `UserServerController`、`UserServerController2`、`UserServerController3` 的同名方法；
  - `getUserById`（DELETE `/user/get/{id}`）额外匹配 `ListenerServerController#getUserById`、`NullServerController#delb`；`ListenerClient#delUsr`、`NullClient#deleUsr` 也在这组里（7 个 Feign 方法 × 5 个 Controller 方法）；
  - 特例：`UserClient3#parallelScan8`（常量路径 `/user/parallelScan9/{id}`）↔ `UserServerController/2/3#parallelScan9`；`UserClient6#updateeeee` ↔ `UserServerController2/3#updateeeee`；`parallelScan12` 无对应 Controller；`test`/`tets` 与 `parallelScan5` 同路径；`parallelScan8`/`parallelScan811` 同路径；`parallelScan10`/`parallelScan11` 只有 `UserServerController`；
  - `ListenerClient#deleUsr` 没有 Rest 注解，不参与匹配。
- 继承场景：`TransportServiceApi`（父接口）↔ `TransportServerController`，经 `TransportService`（path=/hello/world/transport）计算完整路径；GET 对 GET、POST 对 POST。
- ❌ 不匹配：
  - `OrderClient` / `Order2Client` ↔ `OrderServerController` / `Order2ServerController`：客户端缺 `/hello/world` 前缀（如需演示匹配，补 `path = "/hello/world"`）；
  - `UserClientController`、`OrderClientController`、`TestClientController`：消费者 Controller，没有对应的 Feign 端点。

## 怎么测

1. `cd waaagh && ./gradlew runIde`
2. 打开 `MethodMatchClient`：
   - ✅ 行：方法上应出现导航图标，且只指向对应的 Controller 方法；
   - ❌ 行：不应出现导航图标；`Ctrl+Alt+B`（Go to Implementation）结果为空。
3. 反向验证：打开 `MethodMatchServerController`，用 gutter 图标 / `Ctrl+Alt+B` / `Ctrl+Alt+H`（Call Hierarchy）检查匹配到的 Client 方法，与表格互为镜像。
4. 特别检查 #1 / #3：`same-verb` 两个 Controller 方法路径完全相同、仅方法不同，两侧都应只显示正确的那个。
