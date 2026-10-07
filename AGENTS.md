# AGENTS.md

## Development environment

This repo is an **IntelliJ IDEA plugin** called *Waaagh* (Feign ↔ Controller navigator),
living in `waaagh/` (Gradle, Kotlin DSL). `waaagh/sample/debug_openfeign/` is a **Maven** Spring Cloud
sample project used with the sandbox IDE.

### JDK requirement
- Build with **JDK 25**: Gradle **9.7.1** + **IntelliJ Platform Gradle Plugin 2.19.0**,
  targeting **IntelliJ IDEA 2026.2.3** (the unified IDE product since 2025.3).
  The 2026.2 platform requires a matching Java 25 compiler toolchain — JDK 21 is not enough.
- **Gradle is pinned to 9.7.1 on purpose**: Gradle 9.8.0 breaks dependency resolution of
  bundled platform artifacts in Java-only projects (symptom: every `com.intellij.*` import
  is unresolved in the IDE). See JetBrains/intellij-platform-gradle-plugin#2262 — bump once
  the plugin ships a fix.
- Example: `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`
- The Cloud VM snapshot has **JDK 11, 17, and 21** pre-installed under `/usr/lib/jvm/`;
  JDK 25 has to be installed additionally for the Gradle build. JDK 17
  (`java-17-openjdk-amd64`) remains the ideal project SDK for the `sample/debug_openfeign`
  fixture (it targets Java 17).

### Sandbox sample project (`runIde`)
`./gradlew runIde` launches a sandbox of **IntelliJ IDEA 2026.2.3** (the IDE distribution is
downloaded by the IntelliJ Platform Gradle Plugin on first run). For the plugin's
Feign↔Controller navigation to resolve, `waaagh/sample/debug_openfeign` must be imported as a
Maven project with its three modules (`cloud-feign-api`, `cloud-feign-server`,
`feign-order-client`) indexed:
- Project Structure → set the **Project SDK to JDK 17** (`/usr/lib/jvm/java-17-openjdk-amd64`),
  language level 17.
- Maven tool window → **Reload All Maven Projects**. The bundled Maven in 2026.2.3 handles the
  sample's Maven 3.9.7 wrapper fine — the old IDEA 2021.2 import failure no longer applies.
- If newly added sample files don't show up, right-click the sample root → **Reload from Disk**.
- The plugin's scan results are cached per project; the cache is invalidated automatically when
  Java / Spring config files are saved or changed on disk (e.g. by Cursor) and synchronized by
  the IDE — the next navigation or gutter request triggers a rescan. Single-method URL copy is
  always recomputed on the spot.

### Build / lint / run (from `waaagh/`)
- Build: `./gradlew buildPlugin` → `waaagh/build/distributions/waaagh-<version>.zip`
- Override version: `./gradlew buildPlugin -PpluginVersion=1.2.3`
- Verify: `./gradlew verifyPlugin`
- Run sandbox IDE: `./gradlew runIde` (GUI on `DISPLAY=:1`)

### CI
- Workflow `.github/workflows/build-plugin.yml` builds on PR / push to master.
- Tag `v*` creates a GitHub Release with the zip; optional Marketplace publish via `PUBLISH_TOKEN`.

### Verified compatibility
- `since-build` is **203 (2020.3)**. `./gradlew verifyPlugin` checks the plugin against
  **2020.3.4 / 2021.1.3 / 2021.2.4** — all report **Compatible** (verifier 1.410).
- `current()` (2026.2.3) is commented out in the verification matrix: verifying it requires
  JetBrains Marketplace access to resolve transitive plugin dependencies. 2026.2.3 is covered by
  building and running the plugin in the sandbox (`runIde`) instead.

### Sample fixtures
- Matching pair: **`UserClient` → `UserServerController`** (`/hello/world/user/...`).
- Multi-target & shape cases: **`UserDeleteClient`** (second caller), **`UserClientSamePaths`**
  (same-path aliases, client-only negative), **`UserClientConstantPath`** (paths via
  `UserApiPaths` constants) ↔ **`DuplicateUserServerController`** / **`UserDeleteAliasController`**
  (same mappings on purpose); server-side negative **`OrderClientMissingContextPath`**.
- Inheritance: **`InheritanceApi`** (base interface, no `@FeignClient`) ← **`InheritanceFeignClient`**
  (empty `@FeignClient`) ↔ **`InheritanceServerController`** (`/hello/world/inheritance/...`).
- HTTP method matching: **`MethodMatchClient`** ↔ **`MethodMatchServerController`**
  (`/hello/world/method/...`); expected match / no-match matrix in
  `waaagh/sample/debug_openfeign/METHOD_MATCHING.md`.
- Package path prefix: **`WaaaghPathPrefixConfig`** registers `/v2` (lambda over
  `controller.v2*`, startsWith) and `/v3` (`HandlerTypePredicate.forBasePackage`);
  fixtures **`V2UserClient` / `V3OrderClient` / `V2BetaClient`** ↔ **`V2UserApiController` /
  `V3OrderApiController` / `V2BetaApiController`**, negative case **`V2UserClientMissingPrefix`**;
  expected matrix in `waaagh/sample/debug_openfeign/PATH_PREFIX_MATCHING.md`.
