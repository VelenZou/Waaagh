# AGENTS.md

## Cursor Cloud specific instructions

This repo is an **IntelliJ IDEA plugin** called *Waaagh* (Feign ↔ Controller navigator),
living in `waaagh/` (Gradle, Kotlin DSL). `waaagh/sample/debug_openfeign/` is a **Maven** Spring Cloud
sample project used as test-fixture data for the sandbox IDE.

### JDK requirement
- Build with **JDK 11**. Gradle 7.4.2 does not support JDK 21.
- Example: `export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64`

### Build / lint / run (from `waaagh/`)
- Build: `./gradlew buildPlugin` → `waaagh/build/distributions/waaagh-<version>.zip`
- Verify: `./gradlew verifyPlugin`
- Run sandbox IDE: `./gradlew runIde` (GUI on `DISPLAY=:1`)

### Demo navigation
Matching pair: **`UserClient` → `UserServerController`** (`/hello/world/user/...`).
Also exercise **Go to Implementation** (`Ctrl+Alt+B`) and **Call Hierarchy** (`Ctrl+Alt+H`) on Feign methods.
