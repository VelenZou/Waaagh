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

WAAAGH resolves which Spring Cloud `@FeignClient` method matches which Spring MVC `@RestController` / `@Controller` endpoint — by **full path + HTTP method** — and exposes that one mapping through IDEA's own surfaces.

**Editor navigation**

| Capability | How |
|---|---|
| Gutter icons | Matching methods get a jump icon (orange Feign / blue Controller); click it to jump, or pick from the chooser when a method maps to several counterparts |
| **Go to Declaration or Usages** | `Ctrl+B` / `Ctrl+Click` opens IDEA's native usages popup, listing the matching endpoints **and** the real call sites |
| **Go to Implementation** | `Ctrl+Alt+B` lists the matching endpoints, shown with the WAAAGH jump icon |

**Call Hierarchy** (`Ctrl+Alt+H`)

- **Callees (Feign → Controller):** a Feign method lists the matching Controller methods, then keeps walking *into* the server-side implementation.
- **Callers (Controller → Feign):** a Controller method lists the matching Feign clients, then keeps walking *up* through the code that calls them.
- Both trees stay recursive: every node is expanded with its own Java callers / callees.

**Copy URL**

- Click the clipboard gutter icon on a Feign method or a Controller method to copy the complete request URL.

**Endpoints tool window** (IntelliJ IDEA Ultimate)

- The same resolved paths are exposed to IDEA's *Endpoints* view as two frameworks: **Waaagh Feign Client** (HTTP client) and **Waaagh MVC Controllers** (HTTP server).
- Loaded only when the bundled *Endpoints* plugin is present, so Community editions are unaffected.

**Matching**

| Rule | Notes |
|---|---|
| Path + HTTP method | A mapping without an explicit method matches any method |
| Context paths | `server.servlet.context-path`, `spring.mvc.servlet.path` |
| Package prefixes | `PathMatchConfigurer#addPathPrefix` (lambda / `HandlerTypePredicate` / combinations) |
| Inheritance | Endpoint methods declared on a base API interface, reached through an empty `@FeignClient` sub-interface |
| Path expressions | Constants, static fields, local variables, `@Value("${...}")` |
| Shapes | Duplicate mappings and many-to-many relations |
| Caching | Per-project cache, rescanned automatically after save / external change |

#### How it is wired (implementation notes)

- **Call Hierarchy** — a `callHierarchyProvider` (ordered first) wraps `JavaCallHierarchyProvider` and only repurposes the mapping views; everything else stays vanilla Java hierarchy.
- **Go to Declaration or Usages** — a `findUsagesHandlerFactory` is active only for the *Show Usages* popup (`USAGES_WITH_DEFAULT_OPTIONS`): the counterpart method is injected as an extra usage, so IDEA's native popup (signature header, *Merge Usages from the Same Line*, file-path status bar) is preserved. Find Usages (`Alt+F7`) is untouched.
- **Go to Implementation** — a `definitionsScopedSearch` contributes the matching endpoints; their icon comes from the `IconProvider`s below.
- **Icons** — `IconProvider`s paint the WAAAGH jump icon on Feign / Controller methods, via `method.getIcon()`. The Go-to-Implementation popup renders each entry through the Java plugin's own presentation provider, which reads exactly that icon — so no `gotoTargetPresentationProvider` is needed.

## Build

Requires **JDK 25** (Gradle 9.7.1 + IntelliJ Platform Gradle Plugin 2.19, built and tested against IntelliJ IDEA 2026.2.3).

```bash
cd waaagh
export JAVA_HOME=/path/to/jdk-25
./gradlew buildPlugin
```

Output: `waaagh/build/distributions/waaagh-<version>.zip` (override the version with `-PpluginVersion=1.2.3`)

Run sandbox IDE:

```bash
./gradlew runIde
```

## Compatibility

- `since-build` is **231 — IntelliJ IDEA 2023.1**; compatible up to the latest (no `until-build`).
- Verified with `./gradlew verifyPlugin` against **IU 2023.1.7 / IC 2023.1.7** (`Compatible`).
- Requires IntelliJ IDEA 2023.1+ (the Endpoints integration uses an API introduced in that release).
- The **Endpoints** integration additionally needs IntelliJ IDEA Ultimate (the bundled *Endpoints* plugin); everything else also works in Community.

## Sample project

`waaagh/sample/debug_openfeign/` is a Maven multi-module fixture. Matching pair for demos: **`UserClient` → `UserServerController`** (`/hello/world/user/...`).

Shape cases live next to it: **`UserDeleteClient`** (a second caller), **`UserClientSamePaths`** (same-path aliases + a client with no target), **`UserClientConstantPath`** (paths via constants) ↔ **`DuplicateUserServerController`** / **`UserDeleteAliasController`** (deliberately duplicated mappings); inheritance **`InheritanceApi` ← `InheritanceFeignClient`** ↔ **`InheritanceServerController`**; server-side negative **`OrderClientMissingContextPath`**. Full list: `waaagh/sample/debug_openfeign/METHOD_MATCHING.md`.

HTTP method matching has its own verification pair: **`MethodMatchClient` ↔ `MethodMatchServerController`** (`/hello/world/method/...`); the expected match / no-match matrix also lives in `METHOD_MATCHING.md`.

Package-based path prefixes (`PathMatchConfigurer#addPathPrefix`) have their own fixtures: **`V2UserClient` / `V3OrderClient` / `V2BetaClient`** (`/hello/world/v2/...`, `/hello/world/v3/...`), with a negative client missing the prefix (`V2UserClientMissingPrefix`). See `waaagh/sample/debug_openfeign/PATH_PREFIX_MATCHING.md`.

Call Hierarchy recursion has its own fixture: **`HierarchyClient#tree` ↔ `HierarchyServerController#tree`**, with the caller chain `HierarchyCaller#run → #invoke → feign` and the callee chain `controller → HierarchyService#buildUser → #normalize`. See `waaagh/sample/debug_openfeign/CALL_HIERARCHY.md`.

## Contributing

Bug reports, feature requests and pull requests are welcome — please
[open an issue](https://github.com/velenzou/waaagh/issues) or submit a PR.

## License

Apache License 2.0 — see [LICENSE](LICENSE). Third-party attributions are listed in [NOTICE](NOTICE).
