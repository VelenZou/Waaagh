# WAAAGH!!!

[English](README.md) | **中文**

> *Waaagh!* —— 战锤 30k 里兽人（Ork）那股咆哮着的绿色浪潮。他们凭直觉把破铜烂铁拼在一起，结果这玩意儿居然还能跑。本项目就是这种气质。

**WAAAGH!!!** 是一个 IntelliJ IDEA 插件集合。集合里受欢迎的工具，未来可能会被拆分成独立发布的插件。

## 这个项目有什么不一样

本项目的每一行内容 —— 生产代码**和**测试 —— 都由大语言模型端到端生成。**没有人工编写代码，也没有人工 code review**。人类只做两件事：

- 把想法丢给模型，以及
- 对结果做最终验收，凭直觉判断这版行不行。

整个循环就是这样 —— 追求的就是那种 **「感觉可以就可以的那种感觉」**。这正是战锤兽人造东西的方式：抓起手边任何零件，胡乱拼装到一起，吼一嗓子 **WAAAGH!!!**，然后它就莫名其妙地跑起来了。插件的名字也由此而来。

## 集合内容

目前集合里有一个工具。

### Feign ↔ Controller 跳转工具

在 Spring Cloud 的 `@FeignClient` 方法与匹配的 `@RestController` / `@Controller` 接口之间跳转，提供侧边栏图标跳转，以及 IDE 原生的 **Go to Implementation** 和 **Call Hierarchy** 集成。

| 能力 | 用法 |
|---|---|
| 侧边栏图标 | Feign ↔ Controller 双向跳转 + 一键复制 URL |
| **Go to Implementation** | 在 Feign 方法上按 `Ctrl+Alt+B` → 匹配的 Controller |
| **Go to Declaration** | `Ctrl+B` / `Ctrl+单击` 在 Feign 与 Controller 之间跳转 |
| **Call Hierarchy** | Feign 方法把 Controller 显示为**被调用方**；Controller 方法把 Feign 显示为**调用方** |
| 上下文路径 | 解析 `server.servlet.context-path` 与 `spring.mvc.servlet.path` |

#### Hierarchy 是怎么接进去的

- 用一个（`order="first"` 的）`callHierarchyProvider` 包装 `JavaCallHierarchyProvider`，只改写映射相关的视图，其余保持原生 Java hierarchy 行为。
- **Callees（Feign → Controller）：** 在 Feign 方法上打开 Callees，列出匹配的 Controller 方法。
- **Callers（Controller → Feign）：** 在 Controller 方法上打开 Callers，列出匹配的 Feign 客户端方法。
- Feign 客户端可以继承一个基础 API 接口：当父接口承载了 `@RequestMapping` 端点方法、并由一个 `@FeignClient` 子接口继承时，父接口上的这些方法也会被识别为 Feign 方法，跳转与层级都可用。

## 构建

需要 **JDK 11**（Gradle 7.4.2 + IntelliJ 平台 2021.2）。

```bash
cd waaagh
export JAVA_HOME=/path/to/jdk-11
./gradlew buildPlugin
```

产物：`waaagh/build/distributions/waaagh-1.0.0.zip`

启动沙盒 IDE：

```bash
./gradlew runIde
```

## CI / 发布（GitHub Actions）

工作流：[`.github/workflows/build-plugin.yml`](.github/workflows/build-plugin.yml)

| 触发 | 行为 |
|---|---|
| Push `master` / `main`、PR、手动 `workflow_dispatch` | 执行 `buildPlugin`，上传 zip artifact |
| Push tag `v*`（如 `v1.0.0`） | 同上，并创建 **GitHub Release**（附带 zip） |
| Push tag `v*` 且配置了 `PUBLISH_TOKEN` | 额外执行 `publishPlugin` 发布到 JetBrains Marketplace |

发布示例：

```bash
# 版本号取自 tag（去掉 v 前缀），无需手改 build.gradle.kts
git tag v1.0.0
git push origin v1.0.0
```

可选 Secrets（仓库 Settings → Secrets and variables → Actions）：

| Secret | 用途 |
|---|---|
| `PUBLISH_TOKEN` | JetBrains Marketplace 永久令牌（[生成说明](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html#obtaining-a-token)） |
| `CERTIFICATE_CHAIN` / `PRIVATE_KEY` / `PRIVATE_KEY_PASSWORD` | 可选的插件签名（Marketplace 推荐） |

未配置 `PUBLISH_TOKEN` 时，打 tag 发布仍会成功创建 GitHub Release，仅跳过 Marketplace。

## 示例工程

`waaagh/sample/debug_openfeign/` 是一个 Maven 多模块示例工程。演示用的匹配对：**`UserClient` → `UserServerController`**（`/hello/world/user/...`）。

## 许可证

Apache License 2.0 —— 见 [LICENSE](LICENSE)。第三方署名列在 [NOTICE](NOTICE) 中。
