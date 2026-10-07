# WAAAGH!!!

**English** | [中文](README.zh-CN.md)

[![JetBrains Marketplace](https://img.shields.io/jetbrains/plugin/v/33496?label=JetBrains%20Marketplace&logo=jetbrains)](https://plugins.jetbrains.com/plugin/33496)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/33496)](https://plugins.jetbrains.com/plugin/33496)

Install from the IDE (**Settings → Plugins → Marketplace**, search "WAAAGH") or get it on the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/33496).

> *Waaagh!* — the roaring green tide of Warhammer 30k Orks. They bolt scrap together on pure instinct, and somehow the thing *works*. Same energy here.

**WAAAGH!!!** is a collection of IntelliJ IDEA plugins. Popular tools in the collection may later be spun out as their own standalone plugins.

## What's inside

Right now the collection ships one tool.

### Feign ↔ Controller Navigator

Navigates between Spring Cloud `@FeignClient` methods and matching `@RestController` / `@Controller` endpoints, with gutter-icon jumps plus IDE-native **Go to Implementation** and **Call Hierarchy** integration.

| Capability | How |
|---|---|
| Gutter icons | Feign ↔ Controller bidirectional jump + URL clipboard copy |
| **Go to Implementation** | `Ctrl+Alt+B` on a Feign method → matching Controllers |
| **Go to Declaration** | `Ctrl+B` / Ctrl+Click between Feign and Controller |
| **Call Hierarchy** | Feign methods show Controllers as **callees**; Controller methods show Feign clients as **callers** |
| Context path | Parses `server.servlet.context-path` and `spring.mvc.servlet.path` |
| Path prefix | Parses package-based prefixes registered via `PathMatchConfigurer#addPathPrefix`; matching and copied URLs include them |
| Matching | Full path + HTTP method; a mapping without an explicit method matches any |

#### How Hierarchy is wired

- A `callHierarchyProvider` (ordered first) wraps `JavaCallHierarchyProvider` and only repurposes the mapping views; everything else stays vanilla Java hierarchy.
- **Callees (Feign → Controller):** for a Feign method, the Callees tree lists the matching Controller methods.
- **Callers (Controller → Feign):** for a Controller method, the Callers tree lists the matching Feign client methods.
- Feign clients can extend a base API interface: endpoint methods declared on the parent interface (with a `@FeignClient` sub-interface) are treated as Feign methods for navigation and hierarchy.

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

HTTP method matching has its own verification pair: **`MethodMatchClient` ↔ `MethodMatchServerController`** (`/hello/world/method/...`). The expected match / no-match matrix lives in `waaagh/sample/debug_openfeign/METHOD_MATCHING.md`.

Package-based path prefixes (`PathMatchConfigurer#addPathPrefix`) have their own fixtures: **`V2UserClient` / `V3OrderClient` / `V2BetaClient`** (`/hello/world/v2/...`, `/hello/world/v3/...`), with a negative client missing the prefix. See `waaagh/sample/debug_openfeign/PATH_PREFIX_MATCHING.md`.

## Contributing

Bug reports, feature requests and pull requests are welcome — please
[open an issue](https://github.com/velenzou/waaagh/issues) or submit a PR.

## License

Apache License 2.0 — see [LICENSE](LICENSE). Third-party attributions are listed in [NOTICE](NOTICE).
