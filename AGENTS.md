# AGENTS.md

## Cursor Cloud specific instructions

This repo is an **IntelliJ IDEA plugin** called *Waaagh* (Feign ↔ Controller navigator),
living in `waaagh/` (Gradle, Kotlin DSL). `waaagh/sample/debug_openfeign/` is a **Maven** Spring Cloud
sample project used as test-fixture data for the sandbox IDE.

### JDK requirement
- Build with **JDK 11**. Gradle 7.4.2 does not support JDK 21.
- Example: `export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64`
- The Cloud VM snapshot has **JDK 11, 17, and 21** pre-installed under `/usr/lib/jvm/`.
  JDK 11 (`java-11-openjdk-amd64`) is for the Gradle plugin build; JDK 17
  (`java-17-openjdk-amd64`) is the ideal project SDK for the `sample/debug_openfeign`
  fixture (it targets Java 17).

### Sandbox sample project gotcha (important for `runIde` navigation demos)
When you open `waaagh/sample/debug_openfeign` inside the `runIde` sandbox IDE
(IntelliJ IDEA Community **2021.2**), the initial Maven import **fails** with
`NoSuchMethodError: org.apache.maven.model.validation.DefaultModelValidator.<init>()`.
Cause: the sample ships a Maven wrapper pinned to **Maven 3.9.7**
(`.mvn/wrapper/maven-wrapper.properties`), which IntelliJ 2021.2's Maven integration
cannot use. To make the plugin's Feign↔Controller navigation resolve, in the sandbox IDE:
- Settings → Build, Execution, Deployment → Build Tools → Maven → set **Maven home path**
  to **Bundled (Maven 3)** (3.6.3), then reload.
- Project Structure → set the **Project SDK to JDK 17** (`/usr/lib/jvm/java-17-openjdk-amd64`),
  language level 17.
- Maven tool window → **Reload All Maven Projects**.
The plugin's navigation needs the sample's three modules (`cloud-feign-api`,
`cloud-feign-server`, `feign-order-client`) indexed; without a successful Maven import
no gutter icons/navigation targets appear.

### Build / lint / run (from `waaagh/`)
- Build: `./gradlew buildPlugin` → `waaagh/build/distributions/waaagh-<version>.zip`
- Override version: `./gradlew buildPlugin -PpluginVersion=1.2.3`
- Verify: `./gradlew verifyPlugin`
- Run sandbox IDE: `./gradlew runIde` (GUI on `DISPLAY=:1`)

### CI
- Workflow `.github/workflows/build-plugin.yml` builds on PR / push to master.
- Tag `v*` creates a GitHub Release with the zip; optional Marketplace publish via `PUBLISH_TOKEN`.

### Demo navigation
Matching pair: **`UserClient` → `UserServerController`** (`/hello/world/user/...`).
Also exercise **Go to Implementation** (`Ctrl+Alt+B`) and **Call Hierarchy** (`Ctrl+Alt+H`) on Feign methods.
