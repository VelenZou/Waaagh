# WAAAGH!!!

**English** | [中文](README.zh-CN.md)

[![JetBrains Marketplace](https://img.shields.io/jetbrains/plugin/v/33496?label=JetBrains%20Marketplace&logo=jetbrains)](https://plugins.jetbrains.com/plugin/33496)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/33496)](https://plugins.jetbrains.com/plugin/33496)

Install from the IDE (**Settings → Plugins → Marketplace**, search "WAAAGH") or get it on the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/33496).

> *Waaagh!* — the roaring green tide of Warhammer 30k Orks. They bolt scrap together on pure instinct, and somehow the thing *works*. Same energy here.

**WAAAGH!!!** is a collection of IntelliJ IDEA plugins. Popular tools in the collection may later be spun out as their own standalone plugins.

## Why this project is different

Every line in this project — production code **and** tests — is generated end to end by large language models. There is **no hand-written code and no human code review**. Humans do exactly two things:

- throw ideas at the model, and
- give the final thumbs-up on whether the result feels good enough to ship.

That is the whole loop: **if it feels right, it ships** (“感觉可以就可以的那种感觉” — that gut-feel "yeah, this'll do" vibe). It is how a Warhammer Ork builds — grab whatever's lying around, kitbash it together, yell **WAAAGH!!!**, and somehow it runs. Hence the name.

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

## License

Apache License 2.0 — see [LICENSE](LICENSE). Third-party attributions are listed in [NOTICE](NOTICE).
