import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "com.waaagh"
// CI can override via -PpluginVersion=1.2.3 (e.g. from a v* git tag)
version = (findProperty("pluginVersion") as String?) ?: "1.2.2"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // 目标平台：统一版 IntelliJ IDEA（2025.3 起 Community/Ultimate 合并）
        intellijIdea("2026.2.3")
        bundledPlugin("com.intellij.java")
    }

    compileOnly("org.projectlombok:lombok:1.18.22")
    implementation("org.yaml:snakeyaml:1.29")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-collections:commons-collections:3.2.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // 起始支持版本：231 = IntelliJ IDEA 2023.1（以 verifyPlugin 矩阵为准）。
            //
            // 为什么不再支持 203（2020.3）起：Endpoints 图标集成的 provider 实现了
            // com.intellij.microservices.endpoints.EndpointsUrlTargetProvider，该类在
            // 2022.1~2022.3 的 Ultimate 里并不存在（有 microservices 插件、缺这个类），
            // Marketplace 的自动校验因此在 IU 2022.1.4 / 2022.2.5 / 2022.3.3 报
            // “Class not found (1)”。该类自 231 起提供（已在 IU-231.9423.9 实测 Compatible）。
            // 之前本地 verifyPlugin 只测 Community 版，而 IC 上没有可选的
            // com.intellij.microservices.ui，verifier 会跳过这些类，所以从未暴露。
            sinceBuild = "231"
            // 不设兼容上限
            untilBuild = provider { null }
        }
    }

    // 兼容性验证矩阵：最低支持版本 → 各关键节点 → 当前目标版本。
    // 运行 `./gradlew verifyPlugin`；IDE 发行包会在首次运行时下载。
    // 注意：必须用带补丁号的版本（如 2023.1.7）——JetBrains 发布数据里 .0 基础版本的
    // 下载链接为空，会导致 “Couldn't resolve ... download URL” 错误。
    pluginVerification {
        ides {
            // 最低支持版本：必须测 Ultimate —— Endpoints 集成用的是可选的
            // com.intellij.microservices.ui，只在 Ultimate 存在；只测 IC 会因依赖缺失被跳过。
            create(IntelliJPlatformType.IntellijIdeaUltimate, "2023.1.7")
            create(IntelliJPlatformType.IntellijIdeaCommunity, "2023.1.7")
            // current()（2026.2.3）验证需要访问 JetBrains Marketplace 解析传递依赖插件，
            // 在本机直连环境下会失败；2026.2.3 已在沙盒实测，需要时开代理再加回来。
            // current()
        }
    }

    signing {
        // Signing is optional for Marketplace publishing. Only enable it when real credentials are
        // provided; otherwise leave the properties unset so signPlugin is skipped and publishPlugin
        // uploads the unsigned zip. (In CI, unset secrets arrive as EMPTY strings — setting them
        // would make signPlugin run and NPE on a blank private key.)
        val certificateChainEnv = System.getenv("CERTIFICATE_CHAIN")
        val privateKeyEnv = System.getenv("PRIVATE_KEY")
        if (!certificateChainEnv.isNullOrBlank() && !privateKeyEnv.isNullOrBlank()) {
            certificateChain = certificateChainEnv
            privateKey = privateKeyEnv
            password = System.getenv("PRIVATE_KEY_PASSWORD")
        }
    }

    // publishing token 默认取 PUBLISH_TOKEN 环境变量，无需显式配置
}

tasks {
    // 可选的沙盒附加插件：把 marketplace 插件目录放到
    // waaagh/.intellijPlatform/sandbox-extra-plugins/，prepareSandbox_runIde 会连同构建产物
    // 一起同步进沙盒。这样即使普通 `./gradlew runIde` 触发重建（Sync），这些常用插件也不会被清掉。
    // 详见 AGENTS.md 的 “Sandbox sample project” 一节。
    val sandboxExtraPlugins = layout.projectDirectory.dir(".intellijPlatform/sandbox-extra-plugins")
    withType<Sync>().configureEach {
        if (name == "prepareSandbox_runIde") {
            from(sandboxExtraPlugins)
        }
    }

    // 保持 Java 11 字节码，兼容旧版本 IDE 的运行时。
    // 用 --release 而不是 source/target：编译器会同时按 Java 11 的 API 签名校验，
    // 防止误用 JDK 12+ 的 API（这类错误只会在老 IDE 的 JBR 上运行时才暴露）。
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(11)
    }

    withType<Javadoc> {
        options.encoding = "UTF-8"
    }
}
