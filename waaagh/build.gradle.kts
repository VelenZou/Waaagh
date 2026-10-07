import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "com.waaagh"
// CI can override via -PpluginVersion=1.2.3 (e.g. from a v* git tag)
version = (findProperty("pluginVersion") as String?) ?: "1.1.0"

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
            // 起始支持版本：203 = IntelliJ IDEA 2020.3（以 verifyPlugin 矩阵为准）
            sinceBuild = "203"
            // 不设兼容上限
            untilBuild = provider { null }
        }
    }

    // 兼容性验证矩阵：最低支持版本 → 各关键节点 → 当前目标版本。
    // 运行 `./gradlew verifyPlugin`；IDE 发行包会在首次运行时下载。
    // 注意：必须用带补丁号的版本（如 2020.3.4）——JetBrains 发布数据里 .0 基础版本的
    // 下载链接为空，会导致 “Couldn't resolve ... download URL” 错误。
    pluginVerification {
        ides {
            create(IntelliJPlatformType.IntellijIdeaCommunity, "2020.3.4")
            create(IntelliJPlatformType.IntellijIdeaCommunity, "2021.1.3")
            create(IntelliJPlatformType.IntellijIdeaCommunity, "2021.2.4")
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
