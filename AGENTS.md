# AGENTS.md

## Cursor Cloud specific instructions

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
- The plugin's scan results are cached per project; after editing config classes or adding
  Controllers, close/reopen the project to refresh the full scan (single-method URL copy is
  recomputed immediately).

### Build / lint / run (from `waaagh/`)
- Build: `./gradlew buildPlugin` → `waaagh/build/distributions/waaagh-<version>.zip`
- Override version: `./gradlew buildPlugin -PpluginVersion=1.2.3`
- Verify: `./gradlew verifyPlugin`
- Run sandbox IDE: `./gradlew runIde` (GUI on `DISPLAY=:1`)

### CI
- Workflow `.github/workflows/build-plugin.yml` builds on PR / push to master.
- Tag `v*` creates a GitHub Release with the zip; optional Marketplace publish via `PUBLISH_TOKEN`.

### Sample fixtures
- Matching pair: **`UserClient` → `UserServerController`** (`/hello/world/user/...`).
- Inheritance: **`TransportServiceApi`** (base interface, no `@FeignClient`) ← **`TransportService`**
  (empty `@FeignClient`) ↔ **`TransportServerController`** (`/hello/world/transport/...`).
- HTTP method matching: **`MethodMatchClient`** ↔ **`MethodMatchServerController`**
  (`/hello/world/method/...`); expected match / no-match matrix in
  `waaagh/sample/debug_openfeign/METHOD_MATCHING.md`.
- Package path prefix: **`WaaaghPathPrefixConfig`** registers `/v2` (lambda over
  `controller.v2*`, startsWith) and `/v3` (`HandlerTypePredicate.forBasePackage`);
  fixtures **`V2UserClient` / `V3OrderClient` / `V2BetaClient`** ↔ **`V2UserApiController` /
  `V3OrderApiController` / `V2BetaApiController`**, negative case **`V2UserNoPrefixClient`**;
  expected matrix in `waaagh/sample/debug_openfeign/PATH_PREFIX_MATCHING.md`.
