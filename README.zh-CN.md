# WAAAGH!!!

[English](README.md) | **中文**

[![JetBrains Marketplace](https://img.shields.io/jetbrains/plugin/v/33496?label=JetBrains%20Marketplace&logo=jetbrains)](https://plugins.jetbrains.com/plugin/33496)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/33496)](https://plugins.jetbrains.com/plugin/33496)

在 IDE 内安装（**Settings → Plugins → Marketplace**，搜索 “WAAAGH”），或前往 [JetBrains Marketplace 插件页](https://plugins.jetbrains.com/plugin/33496)。

> *Waaagh!* —— 战锤 30k 里兽人（Ork）那股咆哮着的绿色浪潮。他们凭直觉把破铜烂铁拼在一起，结果这玩意儿居然还能跑。本项目就是这种气质。

**WAAAGH!!!** 是一个 IntelliJ IDEA 插件集合。集合里受欢迎的工具，未来可能会被拆分成独立发布的插件。

## 集合内容

目前集合里有一个工具。

### Feign ↔ Controller 跳转工具

WAAAGH 解析出 Spring Cloud 的哪个 `@FeignClient` 方法与 Spring MVC 的哪个 `@RestController` / `@Controller` 端点匹配（按**完整路径 + HTTP 方法**），再把这份映射通过 IDEA 自己的界面呈现出来。

**编辑器内导航**

| 能力 | 用法 |
|---|---|
| 侧边栏图标 | 匹配的方法旁显示跳转图标（Feign 橙 / Controller 蓝），点击即跳；一个方法对应多个目标时弹出选择框 |
| **Go to Declaration or Usages** | `Ctrl+B` / `Ctrl+单击` 打开 IDEA 原生 usages 弹窗，同时列出映射端点**和**真实调用点 |
| **Go to Implementation** | `Ctrl+Alt+B` 列出匹配端点，并带 WAAAGH 跳转图标 |

**Call Hierarchy**（`Ctrl+Alt+H`）

- **Callees（Feign → Controller）：** Feign 方法列出匹配的 Controller，并继续往服务端实现里走。
- **Callers（Controller → Feign）：** Controller 方法列出匹配的 Feign 客户端，并继续上溯调用它们的代码。
- 两棵树都是递归的：每个节点都会继续展开它自己的 Java 调用方 / 被调用方。

**复制 URL**

- 点击 Feign 方法或 Controller 方法旁的剪贴板图标，复制完整请求 URL。

**Endpoints 工具窗口**（IntelliJ IDEA Ultimate）

- 用同一份解析结果向 IDEA 的 *Endpoints* 视图提供两个 framework：**Waaagh Feign Client**（HTTP client）与 **Waaagh MVC Controllers**（HTTP server）。
- 仅在存在内置 *Endpoints* 插件时加载，Community 版不受影响。

**匹配规则**

| 规则 | 说明 |
|---|---|
| 路径 + HTTP 方法 | 未写方法的映射视为通配（匹配任意方法） |
| 上下文路径 | `server.servlet.context-path`、`spring.mvc.servlet.path` |
| 包前缀 | `PathMatchConfigurer#addPathPrefix`（lambda / `HandlerTypePredicate` / 组合写法） |
| 继承 | 声明在基础 API 接口、经空 `@FeignClient` 子接口继承的端点方法 |
| 路径表达式 | 常量、静态字段、局部变量、`@Value("${...}")` |
| 各种形状 | 重复映射、多对多关系 |
| 缓存 | 按项目缓存；保存 / 外部修改同步后自动重扫 |

#### 各功能是怎么接进去的（实现备注）

- **Call Hierarchy** —— 用一个（`order="first"` 的）`callHierarchyProvider` 包装 `JavaCallHierarchyProvider`，只改写映射相关的视图，其余保持原生 Java hierarchy 行为。
- **Go to Declaration or Usages** —— `findUsagesHandlerFactory` 只在 *Show Usages* 弹窗（`USAGES_WITH_DEFAULT_OPTIONS`）下生效：把对端方法作为一条额外 usage 注入，从而保留 IDEA 原生弹窗（签名标题、*Merge Usages from the Same Line* 筛选项、底部文件路径）。Find Usages（`Alt+F7`）不受影响。
- **Go to Implementation** —— `definitionsScopedSearch` 提供匹配端点；图标由下面的 `IconProvider` 提供。
- **图标** —— `IconProvider` 通过 `method.getIcon()` 把 WAAAGH 跳转图标画在 Feign / Controller 方法上。Go to Implementation 弹窗每条目走的是 Java 插件自带的 presentation provider，它取的正是这个图标，所以无需再注册 `gotoTargetPresentationProvider`。

## 构建

需要 **JDK 25**（Gradle 9.7.1 + IntelliJ Platform Gradle Plugin 2.19，构建与实测平台为 IntelliJ IDEA 2026.2.3）。

```bash
cd waaagh
export JAVA_HOME=/path/to/jdk-25
./gradlew buildPlugin
```

产物：`waaagh/build/distributions/waaagh-<version>.zip`（可用 `-PpluginVersion=1.2.3` 覆盖版本号）

启动沙盒 IDE：

```bash
./gradlew runIde
```

## 兼容性

- `since-build` 为 **203 —— IntelliJ IDEA 2020.3**，向上不设上限。
- 已用 `./gradlew verifyPlugin` 对 **2020.3.4 / 2021.1.3 / 2021.2.4** 验证（均 `Compatible`）。
- **Endpoints** 集成额外需要 IntelliJ IDEA Ultimate（内置 *Endpoints* 插件）；其余能力在 Community 版同样可用。

## 示例工程

`waaagh/sample/debug_openfeign/` 是一个 Maven 多模块示例工程。演示用的匹配对：**`UserClient` → `UserServerController`**（`/hello/world/user/...`）。

旁边是各种形状用例：**`UserDeleteClient`**（第二个调用方）、**`UserClientSamePaths`**（同路径多方法 + 无目标客户端）、**`UserClientConstantPath`**（路径写在常量里）↔ **`DuplicateUserServerController`** / **`UserDeleteAliasController`**（故意重复的映射）；继承场景 **`InheritanceApi` ← `InheritanceFeignClient`** ↔ **`InheritanceServerController`**；服务端缺前缀反例 **`OrderClientMissingContextPath`**。完整清单见 `waaagh/sample/debug_openfeign/METHOD_MATCHING.md`。

HTTP 方法匹配有独立的验证对：**`MethodMatchClient` ↔ `MethodMatchServerController`**（`/hello/world/method/...`），应该匹配 / 不应匹配的对照表同样在 `METHOD_MATCHING.md`。

按子包路径前缀（`PathMatchConfigurer#addPathPrefix`）也有独立用例：**`V2UserClient` / `V3OrderClient` / `V2BetaClient`**（`/hello/world/v2/...`、`/hello/world/v3/...`），另有一个故意缺前缀的反例客户端 `V2UserClientMissingPrefix`；对照表见 `waaagh/sample/debug_openfeign/PATH_PREFIX_MATCHING.md`。

Call Hierarchy 的递归也有专门用例：**`HierarchyClient#tree` ↔ `HierarchyServerController#tree`**，调用方链 `HierarchyCaller#run → #invoke → feign`，被调用方链 `controller → HierarchyService#buildUser → #normalize`；对照见 `waaagh/sample/debug_openfeign/CALL_HIERARCHY.md`。

## 参与贡献

欢迎提交 bug 反馈、功能建议和 PR —— 请[提 issue](https://github.com/velenzou/waaagh/issues) 或直接发起 Pull Request。

## 许可证

Apache License 2.0 —— 见 [LICENSE](LICENSE)。第三方署名列在 [NOTICE](NOTICE) 中。
