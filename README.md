# WAAAGH!!!

> *Waaagh!* — Ork psychic energy from Warhammer 30k. Here it powers Feign ↔ Controller navigation in IntelliJ IDEA.

IntelliJ plugin that navigates between Spring Cloud `@FeignClient` methods and matching `@RestController` / `@Controller` endpoints.

Based on [FeignClient Assistant (FeignX)](https://github.com/lltopk/feignx-plugin) (Apache-2.0), Waaagh keeps gutter-icon jumps and adds IDE-native **Go to Implementation** plus **Call Hierarchy** integration (inspired by [MyBatisX](https://gitee.com/baomidou/MybatisX/)).

## Features

| Capability | How |
|---|---|
| Gutter icons | Feign ↔ Controller bidirectional jump + URL clipboard copy |
| **Go to Implementation** | `Ctrl+Alt+B` on a Feign method → matching Controllers |
| **Go to Declaration** | `Ctrl+B` / Ctrl+Click between Feign and Controller |
| **Call Hierarchy** | Feign methods show Controllers as **callees**; Controller methods show Feign clients as **callers** |
| Context path | Parses `server.servlet.context-path` and `spring.mvc.servlet.path` |

### How Hierarchy is wired

- **Callers (Controller → Feign):** `MethodReferencesSearch` contributes synthetic references from matching Feign methods, so the stock Java Call Hierarchy Callers tree picks them up.
- **Callees (Feign → Controller):** a `callHierarchyProvider` (ordered first) wraps `JavaCallHierarchyProvider` and only customizes Feign methods; everything else stays vanilla Java hierarchy.

## Build

Requires **JDK 11** (Gradle 7.4.2 + IntelliJ platform 2021.2).

```bash
cd waaagh
export JAVA_HOME=/path/to/jdk-11
./gradlew buildPlugin
```

Output: `waaagh/build/distributions/waaagh-1.0.0.zip`

Run sandbox IDE:

```bash
./gradlew runIde
```

## CI / Release (GitHub Actions)

Workflow: [`.github/workflows/build-plugin.yml`](.github/workflows/build-plugin.yml)

| 触发 | 行为 |
|---|---|
| Push `master` / `main`、PR、手动 `workflow_dispatch` | `buildPlugin`，上传 zip artifact |
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
| `CERTIFICATE_CHAIN` / `PRIVATE_KEY` / `PRIVATE_KEY_PASSWORD` | 可选插件签名（Marketplace 推荐） |

未配置 `PUBLISH_TOKEN` 时，标签发布仍会成功创建 GitHub Release，仅跳过 Marketplace。

## Sample project

`waaagh/sample/debug_openfeign/` is a Maven multi-module fixture. Matching pair for demos: **`UserClient` → `UserServerController`** (`/hello/world/user/...`).

## License

Apache License 2.0 — see [LICENSE](LICENSE). Original FeignX copyright retained in file headers where applicable.
