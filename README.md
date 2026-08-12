# Waaagh

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

## Sample project

`waaagh/sample/debug_openfeign/` is a Maven multi-module fixture. Matching pair for demos: **`UserClient` → `UserServerController`** (`/hello/world/user/...`).

## License

Apache License 2.0 — see [LICENSE](LICENSE). Original FeignX copyright retained in file headers where applicable.
